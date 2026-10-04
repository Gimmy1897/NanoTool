## NanoTool Dashboard

NanoTool Dashboard is an ImageJ/Fiji plugin for nanoparticle image analysis.

**Current version:** 1.0.2

The dashboard provides:

- automatic scale setup from TEM metadata (`XpixCal`);
- a 2 px radius Mean filter and a persistent count of how many times it was applied;
- the Cell Magic Wand selection tool and its settings;
- ROI counting and nanoparticle measurement with equivalent diameter;
- project management (`Open`, `Save`, `Save As`, and `Close Project`);
- import and export of ROI sets and measurement results;
- automatic synchronization between the ROI Manager and the image overlay.

### Project files

NanoTool projects use the `.ntproj` extension. A project is an ImageJ-compatible TIFF whose extension has been changed to `.ntproj`.

This format keeps the image, ImageJ calibration, ROI overlay, and NanoTool metadata together. It can be opened directly in ImageJ/Fiji by double-clicking the file or dragging it onto the ImageJ/Fiji window.

### Installation

The installer is Windows-only and requires PowerShell, which is included with supported Windows versions.

The latest release files are available in the repository's
[GitHub Releases](https://github.com/Gimmy1897/NanoTool/releases) section.

There are two installation options:

#### Manual installation

Download the following files from the release:

```text
NanoTool_Launcher_Tool.jar
RunAtStartup.ijm
```

Copy them manually to the ImageJ/Fiji installation:

```text
NanoTool_Launcher_Tool.jar -> plugins\Tools
RunAtStartup.ijm            -> macros
```

Restart ImageJ/Fiji after copying the files.

#### PowerShell installer

Alternatively, download the ZIP package from the release. The ZIP contains
these four files in the same folder:

```text
install_nanotool.bat
install_nanotool.ps1
NanoTool_Launcher_Tool.jar
RunAtStartup.ijm
```

Extract the ZIP and run:

```text
install_nanotool.bat
```

The installer:

1. searches common locations for ImageJ and Fiji installations;
2. displays every installation it finds;
3. lets the user select an installation by number;
4. uses option `0` to open a folder selector for a different installation;
5. copies `NanoTool_Launcher_Tool.jar` to `plugins\Tools`;
6. copies `RunAtStartup.ijm` to `macros`.

If the installation is not found automatically, choose `0` and select the ImageJ/Fiji installation folder. Restart ImageJ/Fiji after installation.

`RunAtStartup.ijm` is required to detect `.ntproj` files opened by ImageJ/Fiji and to open the NanoTool dashboard automatically.

### Building

Run `Cell_Magic_Wand_Tool\build.bat` from a Java development environment with ImageJ 1.48 available in the project's `lib` folder. The build creates the launcher JAR and copies `RunAtStartup.ijm` next to the installer files.

The launcher JAR already contains the NanoTool and Cell Magic Wand classes. A separate `Cell_Magic_Wand_Tool.jar` is not required for NanoTool installation.

> NanoTool - made with <3 in Pisa by gimmy1897.dev - October 2026

> Cell Magic Wand plugin created by Theo Walker - January 2014
## Cell Magic Wand 

ImageJ plugin for rapid human-assisted segmentation of cells in images.

### Description

Click on a cell. Get an ROI around the cell.

![demo](img/demo_anim.gif)

### Standalone Cell Magic Wand

Cell Magic Wand is embedded in the NanoTool launcher. A separate
`Cell_Magic_Wand_Tool.jar` is not required when installing NanoTool.

For standalone use, put
[Cell_Magic_Wand_Tool.jar](Cell_Magic_Wand_Tool/Cell_Magic_Wand_Tool.jar)
into the `plugins\Tools` directory of an ImageJ installation and restart
ImageJ. The standalone plugin requires
[ImageJ](https://imagej.nih.gov/ij/download.html) version 1.46d or later.

Select Cell Magic Wand Tool from the ">>" menu on the right side of the toolbar:

![fiji_arrow](img/menu_arrows.png)

Cell Magic Wand will appear in your ImageJ toolbar:

![toolbar](img/ijMainWindow.png)

Try it out on this [test image](img/cells.tif).

### Parameters

Double click on the tool to bring up this box.

![Parameters](img/parameters.png)

**Image Type:** Are your cells brighter or darker than the background color?

**Minimum Diameter:** What's the smallest your cell could be (in pixels)?

**Maximum Diameter:** What's the largest your cell could be (in pixels)?

If you like, you can set the minimum diameter to 1 and the maximum diameter to something huge (like 1000). Cell Magic Wand will still work. But it'll work better the more information you give it.

In particular, setting the maximum diameter lower can prevent Cell Magic Wand from encircling multiple cells at once. Setting the minimum diameter higher helps Cell Magic Wand to ignore high-contrast elements inside your cell, such as nucleoli. And having a smaller range of diameters to search through makes Cell Magic Wand run faster, too.

**Roughness:** This is a shape parameter that controls how rough the edge of your cell ROI will be. Lower it for a smooth ROI that follows the general shape of your cell. Raise it for a rough ROI that delves into every nook and cranny on the outside of your cell. Reasonable values are 0 to 10. A value of 0 will give you a circle -- the smoothest ROI of all.


### Tips

**Shift-clicking:** Hold down SHIFT to select multiple cells. The ROI manager will appear and keep track of your selections. Shift-clicking on a cell you've already selected will deselect it.

**Measure cell diameter:** Use the Line ROI tool to draw a line across your cell. Length will be shown in the ImageJ main window, or you can press Ctrl+M to bring up a measurement. If your units aren't in pixels, you must change the units to pixels first: do Analyze->Set Scale->Click to remove scale, and click OK.

![Measure length](img/measureLength.png)

This will help you determine good values for Minimum and Maximum Diameter.

**Hide ROIs from other slices:** If you're working with an image stack, you may want to make it so that ROIs only show up on the slice they came from. From the ROI manager window, choose More->Options->Associate "Show All" ROIs with slices.

**Median Filtering:** If there is significant contrast variability inside of your cells, try using a median filter to blur out those elements first. Process->Filters->Median.

(1) ![original](img/pv.png) (2) ![median filtered](img/pv-median.png) 

(3) ![median-roi](img/pv-median-roi.png) (4) ![original-roi](img/pv-roi.png) 

(1) Original image, (2) Median-filtered image, (3) ROI drawn on median filtered image, (4) ROI on original image.

**Multichannel and RGB Images:** The first channel, or the red color, will be what Cell Magic Wand sees. Convert images to grayscale or split channels if you want to detect cells in a different channel.

### Credits

Plugin created by Theo Walker.

GCaMP images thanks to William Bosking and Sharon Huang.

Confocal microscopy images thanks to Amanda Jacob and Dominick Casciato.

Cell Magic Wand v1.0 released Jan. 23, 2014. Docs updated Dec. 21, 2020.