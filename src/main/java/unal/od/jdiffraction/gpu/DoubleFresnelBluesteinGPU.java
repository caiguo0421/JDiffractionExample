/*
 * Copyright 2016 Universidad Nacional de Colombia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package unal.od.jdiffraction.gpu;

import java.io.IOException;
import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdevice;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.CUfunction;
import jcuda.driver.CUmodule;
import static jcuda.driver.JCudaDriver.cuLaunchKernel;
import static jcuda.driver.JCudaDriver.cuMemAlloc;
import static jcuda.driver.JCudaDriver.cuMemFree;
import static jcuda.driver.JCudaDriver.cuMemcpyDtoH;
import static jcuda.driver.JCudaDriver.cuMemcpyHtoD;
import static jcuda.driver.JCudaDriver.cuModuleGetFunction;
import static jcuda.driver.JCudaDriver.cuModuleLoad;
import jcuda.jcufft.JCufft;
import static jcuda.jcufft.JCufft.cufftDestroy;
import static jcuda.jcufft.JCufft.cufftPlan2d;
import jcuda.jcufft.cufftHandle;
import jcuda.jcufft.cufftType;
import unal.od.jdiffraction.gpu.utils.ArrayUtilsGPU;
import unal.od.jdiffraction.gpu.utils.CUDAUtils;
import static unal.od.jdiffraction.gpu.utils.CUDAUtils.preparePtxFile;

/**
 * Computes wave diffraction through
 * <a href="http://dx.doi.org/10.1364/AO.49.006430" target="_blank">Fresnel-Bluestein</a>
 * method
 *
 * with double precision.
 *
 * @author Pablo Piedrahita-Quintero (jppiedrahitaq@unal.edu.co)
 * @author Carlos Trujillo (catrujila@unal.edu.co)
 * @author Jorge Garcia-Sucerquia (jigarcia@unal.edu.co))
 *
 * @since JDiffraction 1.2
 */
public class DoubleFresnelBluesteinGPU extends DoublePropagatorGPU {

    private final CUDAUtils cuUtils;

    private static CUcontext context;
    private static CUdevice device;
    private static CUmodule module;

    private static CUfunction multiplication1, multiplication2, multiplication3;

    private static ArrayUtilsGPU utils;

    private static int maxThreads, threadsPerDimension;
    private final int gridX, gridY;

    private final int M, N;
    private final double z, lambda, dx, dy, dxOut, dyOut;
    private final cufftHandle fft;

    /**
     * Creates a new instance of DoubleFresnelBluestein. Also performs kernel
     * calculations.
     *
     * @param M Number of data points on x direction.
     * @param N Number of data points on y direction.
     * @param lambda Wavelength.
     * @param z Distance.
     * @param dx Sampling pitch on x direction.
     * @param dy Sampling pitch on y direction.
     * @param dxOut X pitch on the output field.
     * @param dyOut Y pitch on the output field.
     *
     * @throws IOException
     */
    public DoubleFresnelBluesteinGPU(int M, int N, double lambda, double z, double dx,
            double dy, double dxOut, double dyOut) throws IOException {
        cuUtils = CUDAUtils.getInstance();

        cuUtils.initCUDA();

        device = cuUtils.getDevice(0);

        maxThreads = cuUtils.getMaxThreads(device);
        threadsPerDimension = (int) Math.sqrt(maxThreads);

        gridX = (M + threadsPerDimension - 1) / threadsPerDimension;
        gridY = (N + threadsPerDimension - 1) / threadsPerDimension;

        context = cuUtils.getContext(device);

        declareKernels();

        utils = ArrayUtilsGPU.getInstance();

        this.M = M;
        this.N = N;
        this.lambda = lambda;
        this.dx = dx;
        this.dy = dy;
        this.dxOut = dxOut;
        this.dyOut = dyOut;
        this.z = z;

        fft = new cufftHandle();
        cufftPlan2d(fft, M, N, cufftType.CUFFT_C2C);
    }

    private void declareKernels() throws IOException {
        String filename = preparePtxFile("FresnelBluestein.cu");

        module = new CUmodule();
        cuModuleLoad(module, filename);

        multiplication1 = new CUfunction();
        cuModuleGetFunction(multiplication1, module, "multiplicationFB1D");

        multiplication2 = new CUfunction();
        cuModuleGetFunction(multiplication2, module, "multiplicationFB2D");

        multiplication3 = new CUfunction();
        cuModuleGetFunction(multiplication3, module, "multiplicationFB3D");
    }

    private void multiplication(CUdeviceptr devField, CUfunction multiplication) {
        Pointer parameters = Pointer.to(
                Pointer.to(new int[]{M}),
                Pointer.to(new int[]{N}),
                Pointer.to(new double[]{lambda}),
                Pointer.to(new double[]{z}),
                Pointer.to(new double[]{dx}),
                Pointer.to(new double[]{dy}),
                Pointer.to(new double[]{dxOut}),
                Pointer.to(new double[]{dyOut}),
                Pointer.to(devField)
        );
        cuLaunchKernel(multiplication,
                gridX, gridY, 1,
                threadsPerDimension, threadsPerDimension, 1,
                0, null,
                parameters, null
        );
    }

    @Override
    public void diffract(double[] field) {
        if (field.length != M * 2 * N) {
            throw new IllegalArgumentException("Array dimension must be " + M * 2 * N + ".");
        }

        CUdeviceptr devField = new CUdeviceptr();
        cuMemAlloc(devField, M * 2 * N * Sizeof.DOUBLE);
        cuMemcpyHtoD(devField, Pointer.to(field), M * 2 * N * Sizeof.DOUBLE);

        multiplication(devField, multiplication1);

        JCufft.cufftExecC2C(fft, devField, devField, JCufft.CUFFT_FORWARD);

        multiplication(devField, multiplication2);

        JCufft.cufftExecC2C(fft, devField, devField, JCufft.CUFFT_INVERSE);

        utils.complexShiftGPU(M, N, devField, false);
        multiplication(devField, multiplication3);
        cuMemcpyDtoH(Pointer.to(field), devField, M * 2 * N * Sizeof.DOUBLE);

        cuMemFree(devField);
    }

    @Override
    public void diffract(CUdeviceptr devField) {
        multiplication(devField, multiplication1);

        JCufft.cufftExecC2C(fft, devField, devField, JCufft.CUFFT_FORWARD);

        multiplication(devField, multiplication2);

        JCufft.cufftExecC2C(fft, devField, devField, JCufft.CUFFT_INVERSE);

        utils.complexShiftGPU(M, N, devField, false);
        multiplication(devField, multiplication3);
    }

    public void memFree() {
        cufftDestroy(fft);
    }

    public int getM() {
        return M;
    }

    public int getN() {
        return N;
    }

    public double getZ() {
        return z;
    }

    public double getLambda() {
        return lambda;
    }

    public double getDx() {
        return dx;
    }

    public double getDy() {
        return dy;
    }

    public double getDxOut() {
        return dxOut;
    }

    public double getDyOut() {
        return dyOut;
    }
}
