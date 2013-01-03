apt-s3-jdeb-maven-example
=========================

In a continuously-integrating, horizontally scalable, always available world
we need reliable and convenient ways of distributing our services. What better
way than a debian package? Hosting your own private debian repository means
that service upgrade is just a ``sudo apt-get update && sudo apt-get -y
upgrade`` away. If you're using EC2 for app servers and even a continuous
integration server or two, S3 seems a sensible choice. This project explores
some of the tools to get some of this done.

The example app uses the [Dropwizard](http://dropwizard.codahale.com/)
framework, which bundles Jersey and Jetty to make an uber-jar REST
service. Dropwizard apps are already ops-friendly so it compliments the task
at hand nicely.

Using [jdeb](https://github.com/tcurdt/jdeb), this uber-jar is then packaged
together with maintainer and init scripts into a deb archive. The `jdeb` maven
plugin lets us hook debian package creation into the maven lifecycle.

`jdeb` wants at least a debian `control` file:

    Package: [[artifactId]]
    Version: [[version]]
    Section: misc
    Priority: optional
    Architecture: all
    Maintainer: Tim Lord <tim@izettle.com>
    Description: Awesome App Example
    Depends: debconf (>= 0.5) | debconf-2.0
    PreDepends: dpkg (>= 1.15.7.2)
    Distribution: development

In addition to this, an init.d script is useful, which is placed in
`src/deb/init.d`. The `control` file, as well as
[maintainer scripts](http://www.debian.org/doc/debian-policy/ch-maintainerscripts.html)
(`preinst`, `postinst`, `conffiles`, etc) are by convention placed in the
`src/deb/control` directory.

One advantage of building a debian package comes from being able to use and
specify standard Linux directories for log files (`/var/log`) and
configuration files (`/etc`). Directory mappings and placing files where
they're expected to be, as well as file permissions and user/group is all
configured in the plugin's configuration section in the 
[pom](https://github.com/nzroller/apt-s3-jdeb-maven-example/blob/master/pom.xml). For
example, the following places the app's config file in `/etc`, sets user and
group to `www-data` and makes it solely user read and writable:

    <data>
      <src>${project.basedir}/${project.artifactId}.yaml</src>
      <type>file</type>
      <mapper>
        <type>perm</type>
        <prefix>/etc</prefix>
        <filemode>600</filemode>
        <user>www-data</user>
        <group>www-data</group>
      </mapper>
    </data>


Having come this far, we're actually able to try things out:

    mvn -DperformRelease clean install
	sudo dpkg -i target/*.deb

This will have placed the jar in `/opt`, the init.d script in `/etc/init.d`
and a log directory under `/var/log`.

Upon release, the jar, deb and maven metadata are deployed to S3 using kuali's
[maven-s3-wagon](https://github.com/jcaddel/maven-s3-wagon) (I looked at
[aws-maven](https://github.com/SpringSource/aws-maven) which also provides an
S3 extension but unfortunately it makes all uploaded files public and it's
hardcoded).

`maven-s3-plugin` wants credentials like the following in your (or
on your build server's) `~/.m2/settings.xml`:

    <servers>
      <server>
        <id>aws-release</id>
        <username>AWS_ACCESS_ID</username>
        <password>AWS_SECRET_KEY</password>
        <filePermissions>AuthenticatedRead</filePermissions>
      </server>
      <server>
        <id>aws-snapshot</id>
        <username>AWS_ACCESS_ID</username>
        <password>AWS_SECRET_KEY</password>
        <filePermissions>AuthenticatedRead</filePermissions>
      </server>
    </servers>

Now, for snapshot releases, a ``mvn -DperformRelease deploy`` will deploy to
the `aws-snapshot` repo and for releases the usual `mvn release:prepare`, `mvn
release:perform` will release to `aws-release`.

The final piece of the puzzle is making this maven repository into a hybrid
maven/debian repository. For this I found
[reprepro](http://mirrorer.alioth.debian.org/) the easiest to use. And to sync
'em up we'll use [s3cmd](http://s3tools.org/s3cmd).

First sync your bucket to a local directory:

    mkdir -p ~/repos
    cd ~/repos
    s3cmd sync s3://repos .

`reprepro` wants some repository configuration:

    mkdir snapshot/conf && echo "Codename: snapshot
    Components: main
    Description: Snapshot debian packages
    Architectures: i386 amd64 source
    " > snapshot/conf/distributions

    mkdir release/conf && echo "Codename: release
    Components: main
    Description: Release debian packages
    Architectures: i386 amd64 source
    " > release/conf/distributions

Run `reprepro` for every `deb` found:

    find snapshot/com/ -iname '*.deb' -print0 | xargs -0i reprepro -b snapshot includedeb snapshot {}

    find release/com/ -iname '*.deb' -print0 | xargs -0i reprepro -b release includedeb release {}

This will _copy_ and rename debian packages to `pool` -- I'm sure we could do
better with some symlinks somehow.

Finally sync back the changes:

    s3cmd --dry-run --verbose --follow-symlinks sync . s3://repos

    s3cmd --verbose --follow-symlinks sync . s3://repos

This would probably want to be all hooked up to a build pipeline using a
continuous integration server (Hopefully hosted on EC2 to get those free and
fast transfers).

[apt-s3](https://github.com/pas256/apt-s3) provides a debian S3 transport
method. It is a fork from
[kyleshank/apt-s3](https://github.com/kyleshank/apt-s3) and ironically not
available in public debian/ubuntu repositories. So to build it, `make` will
have to do, on each architecture we're planning on using it on.

Each app server will need this transport method in order to access the private
respository, but once `s3` is built and installed to `/usr/lib/apt/methods/`,
something like the following can be added to `/etc/apt/sources.list`:

``deb s3://AWS_ACCESS_ID:[AWS_SECRET_KEY_INCLUDING_BRACKETS]@s3-eu-west-1.amazonaws.com/BUCKETNAME/snapshot snapshot main``

And that's it! In addition to the usual jar artifacts, we've built and
deployed a debian package using the one command `mvn -DperformRelease
deploy`. We have a private maven/debian hybrid repository hosted on the cheap
and reliable S3. Artifacts are made available for consumption by developers,
testers, QA and your continuous integration servers. On top of that Debian
archives are also made available for the devops team, test and production
servers. Amazon S3 and IAM credentials can be used to fine-tune access.

We could probably have kept the two separate but the idea is to eventually
find out how we can symlink to the debian packages from their maven
locations. We haven't signed the artifacts or packages but hopefully that's
not too hard to setup. An important facet of continuous delivery is the
ability to rollback, and fast. That's something not yet looked at but could
probably be managed by adding
[maintainer scripts](http://www.debian.org/doc/debian-policy/ch-maintainerscripts.html)
in addition to specifying a version when doing the `apt-get install`.

References:

* [Dropwizard](http://dropwizard.codahale.com/)

  Ops-friendly high performance java app "glue" framework

* [tcurdt/jdeb](https://github.com/tcurdt/jdeb)

  Builds debian packages in maven and ant

* [pas256/apt-s3](https://github.com/pas256/apt-s3)

  Provides S3 apt transport method

  The most recent fork of
  [kyleshank/apt-s3](https://github.com/kyleshank/apt-s3)

* [jcaddel/maven-s3-wagon](https://github.com/jcaddel/maven-s3-wagon)

  Provides maven S3 transport method and supports _private_ uploads/access

* [reprepro](http://mirrorer.alioth.debian.org/)

  Builds debian repository metadata

* [s3cmd](http://s3tools.org/s3cmd)

  Command-line interface to S3

* [Private apt repo on S3 tutorial](http://zcox.wordpress.com/2012/08/13/hosting-a-private-apt-repository-on-s3/)

  Tutorial for the debian half of this example
