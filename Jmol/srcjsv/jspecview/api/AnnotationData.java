package jspecview.api;

import jspecview.common.JDXSpectrum;
import jspecview.common.MeasurementData;
import jspecview.common.Parameters;
import jspecview.common.Annotation.AType;

public interface AnnotationData {

	void setSpecShift(double dx);

	AType getAType();
	MeasurementData getData();
	String getGraphSetKey();
	Parameters getParameters();
	JDXSpectrum getSpectrum();
	boolean getState();

	boolean isVisible();

	void setGraphSetKey(String key);
	void setState(boolean b);

}
