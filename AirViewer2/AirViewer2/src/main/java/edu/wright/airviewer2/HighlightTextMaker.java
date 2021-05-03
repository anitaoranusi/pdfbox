/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.wright.airviewer2;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationSquareCircle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 *
 * @author Pabitra Ale
 */
public class HighlightTextMaker {
    public static List<PDAnnotation> make(PDDocument document,
            ArrayList<String> arguments) {
        assert null != arguments && arguments.size() == 5;
        assert null != document;

        List<PDAnnotation> result;

        try {
            int pageNumber = parseInt(arguments.get(0));
            float lowerLeftX = parseFloat(arguments.get(1));
            float lowerLeftY = parseFloat(arguments.get(2));
            float width = parseFloat(arguments.get(3));
            float height = parseFloat(arguments.get(4));
            String contents = "";
            PDFont font = PDType1Font.HELVETICA;
            float fontSize = 16; // Or whatever font size you want.
            float textWidth = font.getStringWidth(contents) * fontSize / 1000.0f;
            float textHeight = 32;

            try {
                PDPage page = document.getPage(pageNumber);
                PDColor red = new PDColor(new float[]{1, 0, 0}, PDDeviceRGB.INSTANCE);
                PDColor color = new PDColor(new float[] { 1, 1 / 255F, 1 }, PDDeviceRGB.INSTANCE);
                PDBorderStyleDictionary borderThick = new PDBorderStyleDictionary();
                borderThick.setWidth(72 / 12 * 0);  // 12th inch
                PDRectangle position = new PDRectangle();
                position.setLowerLeftX(lowerLeftX);
                position.setLowerLeftY(lowerLeftY);
                position.setUpperRightX(lowerLeftX + width);
                position.setUpperRightY(lowerLeftY + height);
                /*position.setLowerLeftX(1000);
                position.setLowerLeftY(1000);
                position.setUpperRightX(1000);
                position.setUpperRightY(1000);
                //markup.setRectangle(position);*/
                

                /*PDAnnotationSquareCircle aSquare = new PDAnnotationSquareCircle(
                        PDAnnotationSquareCircle.SUB_TYPE_SQUARE);
                aSquare.setAnnotationName(UUID.randomUUID().toString());
                aSquare.setContents(contents);
                aSquare.setColor(red);  // Outline in red, not setting a fill
                PDColor fillColor = new PDColor(new float[]{.8f, .8f, .8f}, PDDeviceRGB.INSTANCE);
                aSquare.setInteriorColor(fillColor);
                aSquare.setBorderStyle(borderThick);
                aSquare.setRectangle(position); */
                PDAnnotationTextMarkup aSquare = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);
                aSquare.setRectangle(position); 
                
                /*float w = 300;
                float h = 20;
                float yVal = 600;
                float xVal = 80;
                float quadPoints[] = { xVal, yVal + h, xVal, yVal - h, xVal + w, yVal + h, xVal + w, yVal - h };  */              
                
                float quadPoints[] = {lowerLeftX, lowerLeftY + height, lowerLeftX + width, lowerLeftY + height, lowerLeftX, lowerLeftY, lowerLeftX+width, lowerLeftY}; 
                aSquare.setQuadPoints(quadPoints);
                aSquare.setColor(color);
                
                result = new ArrayList<>(page.getAnnotations()); // copy
                //result = page.getAnnotations();
                page.getAnnotations().add(aSquare);
                //result.add(aSquare);

                // The following lines are needed for PDFRenderer to render 
                // annotations. Preview and Acrobat don't seem to need these.
                if (null == aSquare.getAppearance()) {
                    aSquare.setAppearance(new PDAppearanceDictionary());
                    PDAppearanceStream annotationAppearanceStream = new PDAppearanceStream(document);
                    position.setLowerLeftX(lowerLeftX - borderThick.getWidth() * 0.5f);
                    position.setLowerLeftY(lowerLeftY - borderThick.getWidth() * 0.5f);
                    position.setUpperRightX(lowerLeftX + width + borderThick.getWidth() * 0.5f);
                    position.setUpperRightY(lowerLeftY + height + borderThick.getWidth() * 0.5f);
                    annotationAppearanceStream.setBBox(position);
                    annotationAppearanceStream.setMatrix(new AffineTransform());
                    annotationAppearanceStream.setResources(page.getResources());

                    try (PDPageContentStream appearanceContent = new PDPageContentStream(
                            document, annotationAppearanceStream)) {
                        Matrix transform = new Matrix();
                        appearanceContent.transform(transform);
                        appearanceContent.addRect(lowerLeftX, lowerLeftY, width, height);
                        appearanceContent.setLineWidth(borderThick.getWidth());
                        appearanceContent.setNonStrokingColor(color);
                        appearanceContent.setStrokingColor(color);
                        appearanceContent.fillAndStroke();
                        appearanceContent.beginText();

                        // Center text vertically, left justified
                        appearanceContent.newLineAtOffset(lowerLeftX, lowerLeftY + height * 0.5f - fontSize * 0.5f);
                        appearanceContent.setFont(font, fontSize);
                        appearanceContent.setNonStrokingColor(color);
                        appearanceContent.showText(contents);
                        appearanceContent.endText();
                    }
                    aSquare.getAppearance().setNormalAppearance(annotationAppearanceStream);
                }
                //System.out.println(page.getAnnotations().toString());

            } catch (IOException ex) {
//                Logger.getLogger(DocumentCommandWrapper.class.getName()).log(Level.SEVERE, null, ex);
                result = null;
            }
        } catch (NumberFormatException | NullPointerException ex) {
//            Logger.getLogger(DocumentCommandWrapper.AddBoxAnnotationDocumentCommand.class.getName()).log(Level.SEVERE, null, ex);
            result = null;
        } catch (IOException ex) {
//            Logger.getLogger(BoxAnnotationMaker.class.getName()).log(Level.SEVERE, null, ex);
            result = null;
       }

        return result;
    }
    
}
