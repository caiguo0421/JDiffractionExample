package cn.sf_soft.test;


import ij.ImagePlus;
import ij.gui.GenericDialog;

import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import unal.od.jdiffraction.cpu.FloatAngularSpectrum;
import unal.od.jdiffraction.cpu.utils.ArrayUtils;

import static ij.plugin.filter.PlugInFilter.DOES_ALL;

public class JDiffraction_Example implements PlugInFilter {

    ImagePlus imp;


    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_ALL;
    }


    public void run(ImageProcessor ip) {
        //gets the image data and creates the complex array
        int M = ip.getWidth();
        int N = ip.getHeight();

        float[][] image = ip.getFloatArray();
        float[][] field = ArrayUtils.complexAmplitude2(image, null);

        //unlocks the image because it's no longer needed
        imp.unlock();

        //Creates a GenericDialog object to ask the user for propagation
        //parameters
        GenericDialog gd = new GenericDialog("Parameters");
        gd.addNumericField("Wavelength", 633E-9, 10, 10, "m");
        gd.addNumericField("Distance", 1, 2, 10, "m");
        gd.addNumericField("Input width", 5E-3, 4, 10, "m");
        gd.addNumericField("Input height", 5E-3, 4, 10, "m");
        gd.showDialog();

        if (gd.wasCanceled()){
            return;
        }

        float wavelength = (float) gd.getNextNumber();
        float distance = (float) gd.getNextNumber();
        float inputWidth = (float) gd.getNextNumber();
        float inputHeight = (float) gd.getNextNumber();

        //Creates the AngularSpectrum object and diffracts the input field
        FloatAngularSpectrum as = new FloatAngularSpectrum(M, N, wavelength,
                distance, inputWidth / M, inputHeight / N);
        as.diffract(field);

        //Calculates the modulus of the output field and shows it
        float[][] modulus = ArrayUtils.modulus(field);

        ImageProcessor ipModulus = new FloatProcessor(modulus);
        ImagePlus impModulus = new ImagePlus("modulus", ipModulus);
        impModulus.show();
    }
}