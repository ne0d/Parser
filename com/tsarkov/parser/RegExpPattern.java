package com.tsarkov.parser;

import java.util.regex.Pattern;

public interface RegExpPattern {
    Pattern regexpListNode = Pattern.compile("[a-zA-Z_]\\w*");
    Pattern regexpBranchRec = Pattern.compile("^\\s*\\b[a-zA-Z_][\\w]*\\b\\s+\\=\\s+$");
    Pattern regexpBranchRecLong = Pattern.compile("^\\s*(?:\\b[a-zA-Z_][\\w]*\\b\\s+\\=\\s+[\"“][^\\n\"“”]+\\w[\"”]\\s*)*\\s+\\b[a-zA-Z_][\\w]*\\b\\s+\\=\\s+$");
    Pattern regexpChildrenNode = Pattern.compile("[a-zA-Z_][\\w]*\\s+\\=\\s+[\"“][^\\n\"“”]+[\"”]");
    Pattern regexpListNodeLong = Pattern.compile("\\b[^\\n\"”“]*(?=\\s+\\=\\s*(?![\"“])$)");
    Pattern regexpInnerEndNode = Pattern.compile("^(?:\\s*[a-zA-Z_][\\w]*\\s+\\=\\s+[\"“][^\\n\"“”]+[\"”])+\\s*$");
}
