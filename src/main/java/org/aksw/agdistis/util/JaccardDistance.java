package org.aksw.agdistis.util;

import com.github.jsonldjava.utils.Obj;

import java.util.*;

public class JaccardDistance {

    public static float stringDistance(String str1, String str2) {
        ArrayList<String> arr1 = new ArrayList<>(Arrays.asList(str1.split(" ")));
        ArrayList<String> arr2 = new ArrayList<>(Arrays.asList(str2.split(" ")));

        if (arr1.size() == 0 || arr2.size() == 0)
            return (float) 0.0;

        HashSet<String> set = new HashSet<>();
        int sameCount = 0;

        for (String str: arr1) {
            if (set.contains(str))
                sameCount += 1;
            else
                set.add(str);
        }

        for (String str: arr2) {
            if (set.contains(str))
                sameCount += 1;
            else
                set.add(str);
        }

        return (float) sameCount / (float) set.size();
    }

    public static void main(String[] args) {
        String s1 = "city in the province of Buenos Aires in Argentina";
        String s2 = "Buenos Aires";

        System.out.println(JaccardDistance.stringDistance(s1, s2));
    }
}
