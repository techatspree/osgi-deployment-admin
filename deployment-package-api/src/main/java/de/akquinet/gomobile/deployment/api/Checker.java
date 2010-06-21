package de.akquinet.gomobile.deployment.api;

import java.util.regex.Pattern;


public class Checker {

    public static final Pattern UNIQUE_NAME_PATTERN = Pattern.compile(".+(\\..+)*");

    public static final Pattern PATH_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_\\.-]+");

    public static final String VERSION_PATTERN_STR = "[0-9]+(\\.[0-9]+(\\.[0-9]+(\\.[0-9A-Za-z_-]+)?)?)?";

    public final static Pattern VERSION_PATTERN = Pattern.compile(VERSION_PATTERN_STR);

    public final static Pattern SYMBOLICNAME_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*");

    public final static Pattern VERSIONRANGE_PATTERN = Pattern.compile("((\\(|\\[)"
            + VERSION_PATTERN_STR + "," + VERSION_PATTERN_STR + "(\\]|\\)))|" + VERSION_PATTERN_STR);


    public static boolean checkUniqueName(String value) {
        return UNIQUE_NAME_PATTERN.matcher(value).matches();
    }

    public static boolean checkVersionRange(String value) {
        return VERSIONRANGE_PATTERN.matcher(value).matches();
    }

    public static boolean checkBundleSymbolicName(String value) {
        return SYMBOLICNAME_PATTERN.matcher(value).matches();
    }

    public static boolean checkVersion(String value) {
        return VERSION_PATTERN.matcher(value).matches();
    }

    public static boolean checkPID(String value) {
        //TODO Is it really unique name ?
        return checkUniqueName(value);
    }

    public static boolean checkPathName(String value) {
        return PATH_NAME_PATTERN.matcher(value).matches();
    }





}
