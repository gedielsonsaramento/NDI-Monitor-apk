package br.com.polegar.ndimonitor;

import org.json.JSONObject;
import org.json.JSONTokener;

/** Read-only ES5 probe: no page rewrite, polling, native bridge or network request. */
final class HtmlPageDiagnostics {
    static final String SCRIPT = "(function(){var d=document.documentElement;"
            + "var s=function(p,v){try{return !!(window.CSS&&CSS.supports&&CSS.supports(p,v));}catch(e){return false;}};"
            + "return JSON.stringify({width:window.innerWidth,height:window.innerHeight,"
            + "clientWidth:d.clientWidth,clientHeight:d.clientHeight,dpr:window.devicePixelRatio,"
            + "grid:s('display','grid'),variables:s('color','var(--test)'),"
            + "clamp:s('width','clamp(1px,2px,3px)'),state:document.readyState});})()";

    private HtmlPageDiagnostics() { }

    static String format(String value) {
        try {
            Object decoded = new JSONTokener(value == null ? "null" : value).nextValue();
            JSONObject data = decoded instanceof String ? new JSONObject((String) decoded)
                    : decoded instanceof JSONObject ? (JSONObject) decoded : null;
            if (data == null) return "Página: dados indisponíveis";
            return "Área CSS: " + data.optInt("width") + " × " + data.optInt("height")
                    + "; DPR " + data.optString("dpr", "?")
                    + "\nCSS Grid: " + yesNo(data.optBoolean("grid"))
                    + "; variáveis: " + yesNo(data.optBoolean("variables"))
                    + "; clamp: " + yesNo(data.optBoolean("clamp"))
                    + "\nPágina: " + data.optString("state", "?");
        } catch (org.json.JSONException invalid) {
            return "Página: dados indisponíveis";
        }
    }

    private static String yesNo(boolean value) { return value ? "sim" : "não"; }
}
