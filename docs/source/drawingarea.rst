.. _drawing-area:

======================
Drawing Area
======================

In this settings panel you can configure the output size of your drawing. If you're output is for print for the best results you should enter paper size / pen width to allow DrawingBotV3 to optimise the drawing for plotting.

All changes you make to the drawing size will be automatically update the preview rendered when the "Display Mode" is set to "Image", to apply these changes to the final drawing you should run the PFM again with the **Start** button.

There are a number of presets already included which cover typical paper sizes, you can also create you own presets.

- **Use Original Sizing:** If the original images size should be used, recommended for digital only output, not recommended for print only outputs.
- **Input Units:** To specify the units for the width/height/padding, either mm, cm, inches or pixels
- **Width:** The width of the drawing area, in the current Input Units.
- **Height:** The height of the drawing area, in the current Input Units.
- **Orientation:** Either Portrait/Landscape, if you change presets your current orientation will be maintained.
- **Padding:** The margins of the drawing area, in the order Left/Right/Top/Bottom, in the current Input Units, you can also gang these controls to have a consistent padding size.
- **Scaling Mode:** How to rescale the image to fit into the new dimensions, this resizing occurs before pre-processing so will still be optimised to the paper size / pen width

  - Crop to fit
  - Scale to fit
  - Stretch to fit

- **Rescale to Pen Width**: When activated the image will be rescaled to the specified "pen width".
- **Pen Width (mm)**: The nib size to be used when plotting, this will define the resolution of the image used so where possible use smaller pens or more detailed plots. If you are using multiple sizes often the average of all sizes will give the best result or the most used size.
- **Canvas Color:** Used for changing the background color of your drawing, this is primarily a visual aid if plotting on non-white materials.
- **Clipping Modes:** Allows you to choose if shapes should overflow the edges of the drawing/page or not, this will only have an effect on some PFMS, as some PFMS only work within the image provided anyway. You have three options
    - “Drawing” – Clip the geometries to the drawings edges
    - “Page” – Clip the geometries to the page’s edges
    - “None” – Allow geometries to overflow the page and drawing

