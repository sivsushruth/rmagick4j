#Things to be aware of when coding RMagick4J

# Introduction #

While coding for RMagick4J, I've been notice some things developers should be aware of.


# Details #

  * Opacity in RMagick ranges between 0 (OpaqueOpacity) and MaxRGB (TransparentOpacity). In java.awt.Color, it ranges between 0 (TransparentOpacity) and MaxRGB (OpaqueOpacity). That is the reason why some methods uses 255-opacity in PixelPacket.java.