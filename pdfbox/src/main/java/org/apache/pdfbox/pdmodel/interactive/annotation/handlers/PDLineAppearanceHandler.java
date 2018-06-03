/*
 * Copyright 2018 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pdfbox.pdmodel.interactive.annotation.handlers;

import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLine;
import org.apache.pdfbox.pdmodel.PDAppearanceContentStream;
import org.apache.pdfbox.util.Matrix;

/**
 *
 */
public class PDLineAppearanceHandler extends PDAbstractAppearanceHandler
{
    private static final Log LOG = LogFactory.getLog(PDLineAppearanceHandler.class);

    static final int FONT_SIZE = 9;

    public PDLineAppearanceHandler(PDAnnotation annotation)
    {
        super(annotation);
    }

    @Override
    public void generateAppearanceStreams()
    {
        generateNormalAppearance();
        generateRolloverAppearance();
        generateDownAppearance();
    }

    @Override
    public void generateNormalAppearance()
    {
        PDAnnotationLine annotation = (PDAnnotationLine) getAnnotation();
        PDRectangle rect = annotation.getRectangle();
        float[] pathsArray = annotation.getLine();
        if (pathsArray == null)
        {
            return;
        }
        AnnotationBorder ab = AnnotationBorder.getAnnotationBorder(annotation, annotation.getBorderStyle());
        PDColor color = annotation.getColor();
        if (color == null || color.getComponents().length == 0)
        {
            return;
        }
        float ll = annotation.getLeaderLineLength();
        float lle = annotation.getLeaderLineExtensionLength();
        float llo = annotation.getLeaderLineOffsetLength();

        // Adjust rectangle even if not empty, see PLPDF.com-MarkupAnnotations.pdf
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        for (int i = 0; i < pathsArray.length / 2; ++i)
        {
            float x = pathsArray[i * 2];
            float y = pathsArray[i * 2 + 1];
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        // Leader lines
        if (ll < 0)
        {
            // /LLO and /LLE go in the same direction as /LL
            llo = -llo;
            lle = -lle;
        }

        // add/substract with, font height, and arrows
        // arrow length is 9 * width at about 30° => 10 * width seems to be enough
        // but need to consider /LL, /LLE and /LLO too
        //TODO find better way to calculate padding
        rect.setLowerLeftX(Math.min(minX - Math.max(ab.width * 10, Math.abs(llo+ll+lle)), rect.getLowerLeftX()));
        rect.setLowerLeftY(Math.min(minY - Math.max(ab.width * 10, Math.abs(llo+ll+lle)), rect.getLowerLeftY()));
        rect.setUpperRightX(Math.max(maxX + Math.max(ab.width * 10, Math.abs(llo+ll+lle)), rect.getUpperRightX()));
        rect.setUpperRightY(Math.max(maxY + Math.max(ab.width * 10, Math.abs(llo+ll+lle)), rect.getUpperRightY()));

        annotation.setRectangle(rect);

        try (PDAppearanceContentStream cs = getNormalAppearanceAsContentStream())
        {
            setOpacity(cs, annotation.getConstantOpacity());

            // Tested with Adobe Reader:
            // text is written first (TODO)
            // width 0 is used by Adobe as such (but results in a visible line in rendering)
            // empty color array results in an invisible line ("n" operator) but the rest is visible
            // empty content is like no caption

            boolean hasStroke = cs.setStrokingColorOnDemand(color);

            if (ab.dashArray != null)
            {
                cs.setLineDashPattern(ab.dashArray, 0);
            }
            cs.setLineWidth(ab.width);

            float x1 = pathsArray[0];
            float y1 = pathsArray[1];
            float x2 = pathsArray[2];
            float y2 = pathsArray[3];

            // if there are leader lines, then the /L coordinates represent
            // the endpoints of the leader lines rather than the endpoints of the line itself.
            // so for us, llo + ll is the vertical offset for the line.
            float y = llo + ll;

            String contents = annotation.getContents();
            if (contents == null)
            {
                contents = "";
            }

            double angle = Math.atan2(y2 - y1, x2 - x1);
            cs.transform(Matrix.getRotateInstance(angle, x1, y1));
            float lineLength = (float) Math.sqrt(((x2 - x1) * (x2 - x1)) + ((y2 - y1) * (y2 - y1)));
            if (annotation.hasCaption() && !contents.isEmpty())
            {
                PDType1Font font = PDType1Font.HELVETICA;
                // TODO: support newlines!!!!!
                // see https://www.pdfill.com/example/pdf_commenting_new.pdf
                float contentLength = 0;
                try
                {
                    contentLength = font.getStringWidth(annotation.getContents()) / 1000 * FONT_SIZE;

                    //TODO How to decide the size of the font?
                    // 9 seems to be standard, but if the text doesn't fit, a scaling is done
                    // see AnnotationSample.Standard.pdf, diagonal line
                }
                catch (IllegalArgumentException ex)
                {
                    // Adobe Reader displays placeholders instead
                    LOG.error("line text '" + annotation.getContents() + "' can't be shown", ex);
                }
                float xOffset = (lineLength - contentLength) / 2;
                float yOffset;

                // Leader lines
                cs.moveTo(0, llo);
                cs.lineTo(0, llo + ll + lle);
                cs.moveTo(lineLength, llo);
                cs.lineTo(lineLength, llo + ll + lle);

                String captionPositioning = annotation.getCaptionPositioning();

                // draw the line horizontally, using the rotation CTM to get to correct final position
                // that's the easiest way to calculate the positions for the line before and after inline caption
                if (SHORT_STYLES.contains(annotation.getStartPointEndingStyle()))
                {
                    cs.moveTo(ab.width, y);
                }
                else
                {
                    cs.moveTo(0, y);
                }
                if ("Top".equals(captionPositioning))
                {
                    // this arbitrary number is from Adobe
                    yOffset = 1.908f;
                }
                else
                {
                    // Inline
                    // this arbitrary number is from Adobe
                    yOffset = -2.6f;

                    cs.lineTo(xOffset - ab.width, y);
                    cs.moveTo(lineLength - xOffset + ab.width, y);
                }
                if (SHORT_STYLES.contains(annotation.getEndPointEndingStyle()))
                {
                    cs.lineTo(lineLength - ab.width, y);
                }
                else
                {
                    cs.lineTo(lineLength, y);
                }
                cs.drawShape(ab.width, hasStroke, false);

                // /CO entry (caption offset)
                float captionHorizontalOffset = annotation.getCaptionHorizontalOffset();
                float captionVerticalOffset = annotation.getCaptionVerticalOffset();

                // check contentLength so we don't show if there was trouble before
                if (contentLength > 0)
                {
                    cs.beginText();
                    cs.setFont(font, FONT_SIZE);
                    cs.newLineAtOffset(xOffset + captionHorizontalOffset, 
                                       y + yOffset + captionVerticalOffset);
                    cs.showText(annotation.getContents());
                    cs.endText();
                }

                if (Float.compare(captionVerticalOffset, 0) != 0)
                {
                    // Adobe paints vertical bar to the caption
                    cs.moveTo(0 + lineLength / 2, y);
                    cs.lineTo(0 + lineLength / 2, y + captionVerticalOffset);
                    cs.drawShape(ab.width, hasStroke, false);
                }
            }
            else
            {
                if (SHORT_STYLES.contains(annotation.getStartPointEndingStyle()))
                {
                    cs.moveTo(ab.width, y);
                }
                else
                {
                    cs.moveTo(0, y);
                }
                if (SHORT_STYLES.contains(annotation.getEndPointEndingStyle()))
                {
                    cs.lineTo(lineLength - ab.width, y);
                }
                else
                {
                    cs.lineTo(lineLength, y);
                }
                cs.drawShape(ab.width, hasStroke, false);
            }

            // paint the styles here and not before showing the text, or the text would appear
            // with the interior color
            boolean hasBackground = cs.setNonStrokingColorOnDemand(annotation.getInteriorColor());
            drawStyle(annotation.getStartPointEndingStyle(), cs, 0, y, ab.width, hasStroke, hasBackground);
            drawStyle(annotation.getEndPointEndingStyle(), cs, lineLength, y, ab.width, hasStroke, hasBackground);
        }
        catch (IOException ex)
        {
            LOG.error(ex);
        }
    }

    @Override
    public void generateRolloverAppearance()
    {
        // No rollover appearance generated
    }

    @Override
    public void generateDownAppearance()
    {
        // No down appearance generated
    }
}