## 安装JDK

1. 安装：推荐使用jdk1.8*64位

2. 环境变量配置：配置JAVA_HOME、PATH等环境变量


## 安装IDEA

1. 安装IntelliJ IDEA，请下载Ultimate版本

2. 修改内存：这个程序可能需要比较大的内存，配置内存设置请参考：https://blog.csdn.net/xiprofile/article/details/34102931；主要是修改Xmx参数；

## 设置maven

   1. IntelliJ IDEA Ultimate版本已集成maven，不需要再单独下载和配置

## 配置项目

1. 解压JDiffractionExample项目，idea中File>>open此项目即可

2. 运行： 找到 RunImageJ类，运行main函数即可

## 项目结构

此项目是一个maven项目，maven是一个项目构建工具，pom.xml是其核心配置文件，需要的jar包可以在http://mvnrepository.com/中搜索，

unal.od.jdiffraction中的java文件是从https://github.com/unal-optodigital/JDiffraction?from=singlemessage中直接粘贴过来的

JDiffraction_Example.java是https://unal-optodigital.github.io/JDiffraction/?from=singlemessage中的例子代码



## 参考

JCUDA: http://www.jcuda.org/

imagej: https://imagej.nih.gov/ij/index.html,  这里面可以下载它的集成环境，它这里面集成了java环境（1.8版的jre），里面的ij.jar就是maven中的imageJ

