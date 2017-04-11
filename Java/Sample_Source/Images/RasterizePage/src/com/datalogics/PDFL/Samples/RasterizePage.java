
package com.datalogics.PDFL.Samples;

import com.datalogics.PDFL.*;

public class RasterizePage {

	/*
	 * 
	 * This sample shows how to rasterize a page from a PDF document and save that page as an 
	 * image file. The sample demonstrates using the PageImageParams object with the GetImage
	 * method.
	 *
	 * The program creates three images:
	 *
	 * 1. An output image with a pixel width and resolution that you select. In the example, the default values are 
	 *    400 pixels wide and 300 DPI resolution. The actual size of the image in KB or MB is determined based on
	 *    these two settings.
	 *
	 * 2. An output image half the physical size of a PDF page at a specific resolution. So if the original page is 
	 *    8.5 x 11 inches, the size of a JPG or PNG output file would be 4.25 x 5.5 inches. The method used in this
	 *    sample, CreatePageImageBasedOnPhysicalSize, does not provide an input for the physical size of the output
	 *    image file. Rather, you can enter a scale value (defaults to .5) and a resolution (defaults to 96 DPI).
	 *    The example show how to calculate the width, in pixels, of the resulting graphics image file.
	 *
	 * 3. An output image file with content drawn from an unrotated PDF page, but that contains only the top half of
	 *    the original page.
	 *
	 * Copyright (c) 2007-2017, Datalogics, Inc. All rights reserved.
	 *
	 * For complete copyright information, refer to:
	 * http://dev.datalogics.com/adobe-pdf-library/license-for-downloaded-pdf-samples/
	 *
	 */
	
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("RasterizePage Sample:");

        Library lib = new Library();

        try {
            System.out.println("Initialized the library.");
            
            String sInput = "../../Resources/Sample_Input/ducky.pdf";
            String sOutput = "RasterizePage";

            if (args.length > 0)
                sInput = args[0];

            if (args.length > 1)
                sOutput = args[1];

            System.out.println("Using input file " + sInput + " writing to output with prefix " + sOutput);

            Document doc = new Document(sInput);

            Page pg = doc.getPage(0);

            /////////////////////////////////////////////////////////
            //
            //  First, we'll make an image that is exactly 400 pixels
            //  wide at a resolution of 300 DPI.
            //  
            ////////////////////////////////////////////////////////

            // Create a PageImageParams with the default settings.
            PageImageParams pip = new PageImageParams();

            // Set the PixelWidth to be exactly 400 pixels.
            // We don't need to set the PixelHeight, as DLE will calculate
            // this for us automatically based on the PixelWidth and resolution.
            //
            // If you'd like a specific height, you can specify that too.
            // Be aware that the image may end up looking warped if the aspect ratio 
            // of the specified PixelWidth and PixelHeight don't match the a
            // aspect ratio of the original.
            pip.setPixelWidth(400);

            // Since the default resolution is 300 DPI, we don't need to explicitly
            // set the HorizontalResolution and VerticalResolution in this case.
            //
            // Again, we'll create an image of the entire page using the page's
            // CropBox as the exportRect.  The default ColorSpace is DeviceRGB, 
            // so the image will be DeviceRGB.
            Image inputImage = pg.getImage(pg.getCropBox(), pip);
            inputImage.save( sOutput + "-400pixel-width.jpg", ImageType.JPEG);
            System.out.println("Created " +  sOutput + "-400pixel-width.jpg...");                

            /////////////////////////////////////////////////////////
            //
            //  Next, we'll make a grayscale image that is half the 
            //  physical size of the page at 96 DPI.
            //  
            ////////////////////////////////////////////////////////

            createPageImageBasedOnPhysicalSize(pg, sOutput + "-grayscale-halfsize.jpg", ImageType.JPEG, ColorSpace.DEVICE_GRAY);


            /////////////////////////////////////////////////////////
            //
            //  Next, we'll make an image that contains just the
            //  top half of the page at a resolution of 300 DPI.
            //  
            ////////////////////////////////////////////////////////

            createPageImageWithTopHalfOfPage(pg, sOutput + "-tophalf.jpg", ImageType.JPEG, ColorSpace.DEVICE_RGB);

        }
        finally {
            lib.delete();
        }
    }

    public static void createPageImageBasedOnPhysicalSize(Page pg, String filename, ImageType imgtype, ColorSpace cspace)
    {
        // Get the dimensions, in pixels, of an image that is 
        // half the physical size of the page at a resolution of 96 DPI.
        // The dimensions will be stored in the PixelWidth and
        // PixelHeight fields of the PageImageParams object.
        double scalefactor = 0.5;
        double resolution = 96.0;
        PageImageParams pip = scalePage(pg, scalefactor, resolution);
        pip.setImageColorSpace(cspace);

        // Create the image and save it to a file.
        // We want to create an image of the entire page, so we'll use the
        // page's CropBox as the exportRect.
        Image img = pg.getImage(pg.getCropBox(), pip);
        img.save(filename, imgtype);
        System.out.println("Created " + filename + "...");
    }

    public static PageImageParams scalePage(Page pg, double scalefactor, double resolution)
    {
        // The page's CropBox defines the area of the PDF page intended to be
        // displayed or printed.  Some PDF pages may have extra data that lies
        // outside the boundaries of the CropBox -- if the page's MediaBox is
        // used, this extra data may be captured.
        //
        // So we will use the CropBox to determine the width and height of 
        // the displayed page.  Keep in mind that the width and height
        // obtained from the page's CropBox is in user units, which are 
        // usually 1/72 physical units (inches)
        //
        // The page's CropBox refers to an unrotated page.  If the page
        // has a Rotate value of 90 or 270, the userWidth should be 
        // calculated from the y-values and the userHeight should be
        // calculated from the x-values.
        double userWidth; 
        double userHeight;

        if ((pg.getRotation() == PageRotation.ROTATE_90) || (pg.getRotation() == PageRotation.ROTATE_270))
        {
            userWidth = pg.getCropBox().getURy() - pg.getCropBox().getLLy();
            userHeight = pg.getCropBox().getURx() - pg.getCropBox().getLLx();
        }
        else
        {
            userWidth = pg.getCropBox().getURx() - pg.getCropBox().getLLx();
            userHeight = pg.getCropBox().getURy() - pg.getCropBox().getLLy();
        }

        // To get the physical height of the page, divide the width and
        // height in user units by 72.  
        double physWidth = userWidth / 72.0;
        double physHeight = userHeight / 72.0;

        // Reduce the physical size by the scaling factor.
        physWidth = physWidth * scalefactor;
        physHeight = physHeight * scalefactor;

        // Multiply the physical width and height by the desired resolution to get the
        // pixel width and height of the image.  We'll create a PageImageParams object
        // to store this information.
        PageImageParams pip = new PageImageParams();
        pip.setPixelWidth((int)(physWidth * resolution));
        pip.setPixelHeight((int)(physHeight * resolution));
        pip.setHorizontalResolution(resolution);
        pip.setVerticalResolution(resolution);

        return pip;

    }

    public static void createPageImageWithTopHalfOfPage(Page pg, String filename, ImageType imgtype, ColorSpace cspace)
    {
        // Create a PageImageParams with the default settings and set
        // the color space as appropriate.
        // We'll let DLE decide the final pixel dimensions of
        // the bitmap, so we won't change these settings from the default.

        PageImageParams pip = new PageImageParams();
        pip.setImageColorSpace(cspace);

        // Set up our exportRect to define the drawing area.
        //
        // Since we want the top half of the page only, we'll shift the
        // y coordinates of the lower left corner.
        //
        // The exportRect and the CropBox are both in user space coordinates, where
        // 1 unit in user space is equal to 1/72 in.  Keep this in mind when
        // defining your exportRect if you want to export only part of the page.
        //
        // Also remember that the CropBox corresponds to the unrotated page.
        // If the top half of the rotated page is needed, check the page's
        // rotation value and adjust your Rect calculations accordingly.

        Rect cBox = pg.getCropBox();
        Rect topHalfOfRotatedPage = cBox;

        PageRotation r = pg.getRotation();

        if(r == PageRotation.ROTATE_90)
        {
            // In this case, choose a rectangle that covers the left half
            // of the unrotated page (this corresponds to top half of rotated page)
            topHalfOfRotatedPage.setURx(cBox.getURx() - ((cBox.getURx() - cBox.getLLx()) / 2));
        }
        else if(r == PageRotation.ROTATE_180)
        {
            // In this case, choose a rectangle that covers the bottom half
            // of the unrotated page (this corresponds to top half of rotated page)
            topHalfOfRotatedPage.setURy(cBox.getURy() - ((cBox.getURy() - cBox.getLLy()) / 2));
        }
        else if(r == PageRotation.ROTATE_270)
        {
            // In this case, choose a rectangle that covers the right half
            // of the unrotated page (this corresponds to top half of rotated page)
            topHalfOfRotatedPage.setLLx(cBox.getLLx() + ((cBox.getURx() - cBox.getLLx()) / 2));
        }
        else
        {
            // No rotation, use the top half of the page.
            topHalfOfRotatedPage.setLLy(cBox.getLLy() + ((cBox.getURy() - cBox.getLLy()) / 2));
        }

        // Create the image.
        Image img = pg.getImage(topHalfOfRotatedPage, pip);
        img.save(filename, imgtype);
        System.out.println("Created " + filename + "...");
    }

}