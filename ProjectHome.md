**Moved to [Github](http://github.com/Serabe/RMagick4J)**


---


RMagick is a Ruby binding to ImageMagick and GraphicsMagick. RMagick4J implements ImageMagick functionality and the C portions of RMagick for use with JRuby.

RMagick4J source is public domain, but it includes addition software. See LicenseInformation for more on the licenses.

You can install the gem at RubyForge like so:

```
gem install rmagick4j
```

And then you can use it in your software like so:

```
require 'rubygems'
gem PLATFORM == 'java' ? 'rmagick4j' : 'rmagick'
require 'RMagick'
```

In a future version, the gem may be released simply as rmagick (well, rmagick-java), but for now the support is sufficiently limited that it has been deemed best to use a different name to make people aware of the difference.