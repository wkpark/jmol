package jspecview.api;

import javajs.util.List;

public interface JSVAppInterface extends JSVAppletInterface, ScriptInterface {

	List<String> getScriptQueue();

}
