package jspecview.api;

import javajs.util.List;
import jspecview.common.JSViewer;


public interface ExportInterface extends JSVExporter {

	/**
	 * from EXPORT command
	 * 
	 * @param viewer
	 * @param tokens
	 * @param forInkscape 
	 * @return message for status line
	 */
	String write(JSViewer viewer, List<String> tokens,
			boolean forInkscape);

}