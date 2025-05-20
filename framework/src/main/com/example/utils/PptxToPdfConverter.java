package com.example.utils;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
@Component
public class PptxToPdfConverter {

    public static File convertPptxToPdf(File pptxFile, File outputPdfFile) throws Exception {
        try (FileInputStream inputStream = new FileInputStream(pptxFile);
             XMLSlideShow ppt = new XMLSlideShow(inputStream);
             PDDocument pdf = new PDDocument()) {

            Dimension pgsize = ppt.getPageSize();

            for (XSLFSlide slide : ppt.getSlides()) {
                // 将每一页幻灯片绘制成图像
                BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = img.createGraphics();
                graphics.setPaint(Color.white);
                graphics.fill(new Rectangle(pgsize));
                slide.draw(graphics);

                // 转为 PDF 页面
                PDPage page = new PDPage(new PDRectangle(pgsize.width, pgsize.height));
                pdf.addPage(page);

                // 插入图像
                var contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page);
                var pdImage = LosslessFactory.createFromImage(pdf, img);
                contentStream.drawImage(pdImage, 0, 0, pgsize.width, pgsize.height);
                contentStream.close();
            }

            // 保存 PDF 文件
            pdf.save(outputPdfFile);
            return outputPdfFile;
        }
    }
}
