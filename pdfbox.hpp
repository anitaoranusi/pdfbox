\package <edu.wright.airviewer2>
#ifndef pdfbox_hpp
#define pdfbox_hpp
#include<stdio.h>

#include <string>
/**
 * \defgroup HighlightText
 * @{
 */
/// Ref:Task008.3 
/// This class highlight text in desire area.

class HighlightText{
    public:

        public static List<PDAnnotation> make(PDDocument document,
            ArrayList<String> arguments); /// Ref:Task008.3 
};


/**@}*/
#endif