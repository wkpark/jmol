package jspecview.common;

import java.io.OutputStream;

interface PdfCreatorInterface {

	void createPdfDocument(AwtPanel awtPanel, PrintLayout pl, OutputStream os);
}
