# Drawing Bot V3

![Screenshot](https://github.com/SonarSonic/Drawbot_image_to_gcode_v3/blob/master/images/ScreenshotV101.PNG?raw=true)
[![Platforms](https://img.shields.io/badge/platform-Windows%2C%20Mac%2C%20Linux-green?style=flat-square)](https://github.com/SonarSonic/DrawingBotV3#installation)
![GitHub top language](https://img.shields.io/github/languages/top/SonarSonic/DrawingBotV3?style=flat-square)
[![GitHub License](https://img.shields.io/github/license/SonarSonic/DrawingBotV3?style=flat-square)](https://github.com/SonarSonic/DrawingBotV3/blob/master/LICENSE)
[![GitHub release (latest by date including pre-releases)](https://img.shields.io/github/v/release/SonarSonic/DrawingBotV3?include_prereleases&style=flat-square)](https://github.com/SonarSonic/DrawingBotV3/releases/latest)
![GitHub all releases](https://img.shields.io/github/downloads/SonarSonic/DrawingBotV3/total?style=flat-square)
[![Documentation Status](https://readthedocs.org/projects/drawingbotv3/badge/?version=latest)](https://drawingbotv3.readthedocs.io/en/latest/?badge=latest)

### About

Drawing Bot is a free, open source software for converting images to line drawings for Plotters / Drawing Machines / 3D printers. It also serves as an application for visual artists to create stylised line drawings from images / video.

It is available for Windows, Mac and Linux.

If you want to support the development of DrawingBotV3 you can [donate here](https://www.paypal.com/donate?hosted_button_id=ZFNJF2R4J87DG)

### Features

-  [15 Path Finding Algorithms](https://drawingbotv3.readthedocs.io/en/latest/pfms.html) - all highly configurable to create unique drawing styles
-  Automatic Path Optimisation for Faster Plots - Line Simplifying, Merging, Filtering, Sorting
-  [Pen Settings](https://drawingbotv3.readthedocs.io/en/latest/pensettings.html): configurable colour / stroke width / distribution weight / blend modes - perfect for multi-layered plots.
-  [Image Sequences](https://drawingbotv3.readthedocs.io/en/latest/exportsettings.html#image-sequence-settings): You can export image sequences animations of your creations!
-  Version Control: Save your favourite versions as you go and reload them.
-  Project Saving & Loading
-  [60+ Image Filters](https://drawingbotv3.readthedocs.io/en/latest/preprocessing.html) for pre processing the imported image
-  Automated [CMYK separation](https://drawingbotv3.readthedocs.io/en/latest/cmyk.html)
-  [Advanced User Interface](https://drawingbotv3.readthedocs.io/en/latest/userinterface.html) with live drawing preview
-  User configurable Drawing Area, with Padding / Scaling Modes 
-  [Special pens](https://drawingbotv3.readthedocs.io/en/latest/pensettings.html#special-drawing-pens) for Original Colour/Grayscale Sampling
-  [Presets](https://drawingbotv3.readthedocs.io/en/latest/presets.html): can be saved/imported/exported for sharing different styles with other users
-  Exports can be exported per/pen or per/drawing in multiple file types
-  [Batch Processing](https://drawingbotv3.readthedocs.io/en/latest/batchprocessing.html): Convert entire folders of images automatically.
-  [GCode](https://drawingbotv3.readthedocs.io/en/latest/exportsettings.html#gcode-settings) - configurable Drawing Area, XYZ Offsets / Auto Homing.
-  [vpype](https://github.com/abey79/vpype) Integration

### Path Finding Modules
- Sketch Lines PFM
- Sketch Curves PFM 
- Sketch Squares PFM
- Sketch Quad Beziers PFM
- Sketch Cubic Beziers PFM
- Sketch Catmull-Roms PFM
- Sketch Shapes PFM
- Sketch Sobel Edges PFM
- Spiral PFM
- Voronoi Circles
- Voronoi Triangulation
- Voronoi Stippling
- Voronoi Diagram
- Mosaic Rectangles
- Mosaic Voronoi

More info [here](https://drawingbotv3.readthedocs.io/en/latest/pfms.html)

#### Supported File Types

```text
Import Formats: 
    Images: [.tif, .tga, .png, .jpg, .gif, .bmp, .jpeg] 
       
Export Formats: 
    Vectors: [.svg, .pdf],
    Images/Image Sequences: [.tif, .tga, .png, .jpg, .jpeg]
    GCode: [.gcode, .txt],
```

# Installation

Downloads: [Latest Release](https://github.com/SonarSonic/DrawingBotV3/releases/latest)

You can choose from the following options.

1) **Windows - Installer** _(.exe)_
        
   Includes all required libraries and Java Runtime. No further setup required.
   
2) **Windows - Portable** _(.zip)_
   
   Includes all required libraries and Java Runtime. No further setup required.

3) **Mac (x86)/Linux/Win - Executable** _(.jar)_ 

   Includes all required libraries but you must manually install [JAVA 11+](https://www.oracle.com/java/technologies/javase-downloads.html)
   
4) **Mac M1 (arm64) - Executable** _(.jar)_ 

   The bundled OpenJFX does not work on arm64 processors and using a x86 java build (through Rosetta 2 emulation) has graphical glitches & reduced performance. 
   The best option is to install a JDK build with JFX built in, such as the one provided by [Bellsoft](https://github.com/bell-sw/homebrew-liberica) just make sure to install the full package or JFX won't be bundled.

5) **Raspberry PI (ARM32) - Executable** _(.jar)_ 

   As JavaFX is no longer part of the JDK (since JAVA 11), running a JavaFX program on Raspberry Pi will not work.<br>
   BellSoft provides the [Liberica JDK](https://bell-sw.com/pages/downloads/#/java-11-lts). The version dedicated for the Raspberry Pi includes JavaFX. And setting the version by default using the update-alternatives command.<br>
   Thanks to [Frank Delporte](https://github.com/FDelporte), more info at [Java Magazine](https://blogs.oracle.com/javamagazine/getting-started-with-javafx-on-raspberry-pi)
```text
    $ cd /home/pi 
    $ wget https://download.bell-sw.com/java/13/bellsoft-jdk13-linux-arm32-vfp-hflt.deb 
    $ sudo apt-get install ./bellsoft-jdk13-linux-arm32-vfp-hflt.deb 
    $ sudo update-alternatives --config javac 
    $ sudo update-alternatives --config java
```

### Running the (.jar) on a MAC

Sometimes opening the .jar normally won't work on MAC, instead you should launch the jar from the terminal with the following command. Swapping in the correct file name
```text
    java -jar DrawingBotV3-X.X.X-XXXX-all.jar
```
### Running the (.jar) on a Raspberry PI

Opening the .jar may open it as an archive file, instead you should launch the jar from the terminal with the following command. Swapping in the correct file name
```text
    java -jar DrawingBotV3-X.X.X-XXXX-all.jar
```




### Included Dependencies

All the dependencies are automatically included and **do not need to be installed manually**.

- [OpenJFX](https://github.com/openjdk/jfx) - for User Interface / Rendering
- [JTS Topology Suite](https://github.com/locationtech/jts) - for Vectors/Geometry
- [ImgScalr](https://github.com/rkalla/imgscalr) - for Optimised Image Scaling
- [Gson](https://github.com/google/gson) - for Configuration/Preset Files
- [Apache XML Graphics](https://github.com/apache/xmlgraphics-batik) - for SVG Rendering
- [iText](https://github.com/itext/itextpdf) - for PDF Rendering
- [FXGraphics2D](https://github.com/jfree/fxgraphics2d) - for Swing/JavaFX Compatibility
- [JHLabs](http://www.jhlabs.com/) - for Image Filters / Effects

---

### Original Version

DrawingBotV3 started as an expansion of [Drawbot Image to GCode V2](https://github.com/Scott-Cooper/Drawbot_image_to_gcode_v2) originally written by **Scott Cooper**.

Thanks to Scott for allowing me to publish this version!
