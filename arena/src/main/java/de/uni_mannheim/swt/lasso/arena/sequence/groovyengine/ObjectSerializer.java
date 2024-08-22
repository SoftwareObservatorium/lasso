package de.uni_mannheim.swt.lasso.arena.sequence.groovyengine;

import com.google.gson.Gson;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 *
 * @author Marcus Kessel
 */
public class ObjectSerializer {
    static Gson gson = new Gson();

    public static Object serialize(Object val) {
//        if(val == null) {
//            return "null";
//        }

        //return ToStringBuilder.reflectionToString(val, ToStringStyle.JSON_STYLE);

        return gson.toJson(val);
    }
}
