package cn.sf_soft.test;

import ij.IJ;
import ij.ImageJ;

public class RunImageJ {

    public static void main(String[] args) {
        //设置为debug模式
        IJ.setDebugMode(true);
        //运行ImageJ
        ImageJ imageJ = new ImageJ();

        //或者调用ImageJ的main函数执行
//        ImageJ.main(args);

    }

}
