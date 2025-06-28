package io.github.jsbxyyx;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CheckDepLicCli {

    static Map<String/* license */, List<String>> dep_lic = new LinkedHashMap<>();

    static Map<String, List<String>> result = new LinkedHashMap<>();

    public static void main(String[] args) {

        args = new String[]{"C:\\Users\\test\\IdeaProjects\\seata-depoly\\0606\\seata-namingserver\\lib"};
//        args = new String[]{"C:\\Users\\test\\IdeaProjects\\seata-depoly\\0606\\seata-server\\lib"};

        System.out.println(Arrays.toString(args));

        lic();

        dep(args);

        result.forEach((k, v) -> {
            String join = Joiner.on("\n").join(v);
            try {
                Files.write(join.getBytes(StandardCharsets.UTF_8), new File(System.getProperty("user.dir") + "/licenses/" + k));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    static void lic() {
        try {
            List<String> strings = Files.readLines(new File(System.getProperty("user.dir") + "/LICENSE"), StandardCharsets.UTF_8);
            strings.forEach((str) -> {
                if (str.length() > 5 && str.startsWith("    ") && str.charAt(4) != ' ') {
                    List<String> split = Splitter.on(" ").trimResults().omitEmptyStrings().splitToList(str.trim());
                    if (split.size() >= 3) {
                        String license = split.get(2);
                        if (dep_lic.containsKey(license)) {
                            dep_lic.get(license).add(str.trim());
                        } else {
                            List<String> list = new ArrayList<>();
                            list.add(str.trim());
                            dep_lic.put(license, list);
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void dep(String[] args) {
        File file = new File(args[0]);
        try {
            File[] files = file.listFiles();
            if (files != null) {
                int licenseCount = 0;
                int noticeCount = 0;
                int fileCount = 0;
                for (File f : files) {
                    if (f.isDirectory()) {
                        continue;
                    }
                    if (f.getName().startsWith("seata-")) {
                        continue;
                    }
                    fileCount++;
                    JarFile jarFile = new JarFile(f);
                    Enumeration<JarEntry> entries = jarFile.entries();

                    boolean existsPom = false;
                    boolean existsNotice = false;
                    String artifactId = "";
                    String groupId = "";
                    String version = "";

                    while (entries.hasMoreElements()) {
                        JarEntry jarEntry = entries.nextElement();
                        String entryName = jarEntry.getName();
                        if (entryName.startsWith("META-INF/maven/") && entryName.endsWith("pom.properties") && !existsPom) {
                            try (InputStream in = jarFile.getInputStream(jarEntry)) {
                                Properties props = new Properties();
                                props.load(in);
                                artifactId = props.getProperty("artifactId");
                                groupId = props.getProperty("groupId");
                                version = props.getProperty("version");
                                if (entryName.contains(artifactId)) {
                                    existsPom = true;
                                }
                            }
                        } else if (entryName.startsWith("META-INF/") && entryName.toLowerCase().contains("notice")) {
                            try (InputStream in = jarFile.getInputStream(jarEntry)) {
                                byte[] b = ByteStreams.toByteArray(in);
                                Files.write(b, new File(System.getProperty("user.dir") + "/notices/" + f.getName().replace(".jar", "") + "-notice.txt"));
                                noticeCount++;
                                existsNotice = true;
                            }
                        }
                    }
                    if (!existsPom) {
                        String name = f.getName().replace(".jar", "");

                        int dotIndex = name.indexOf('.');
                        int dashIndex = name.lastIndexOf('-', dotIndex);

                        if (dashIndex == -1) {
                            dashIndex = name.lastIndexOf('-');
                        }

                        artifactId = name.substring(0, dashIndex);
                        version = name.substring(dashIndex + 1);
                    }

                    final String finalArtifactId = artifactId;
                    final String finalGroupId = groupId;
                    final String finalVersion = version;

                    boolean found = false;
                    for (Map.Entry<String, List<String>> entry : dep_lic.entrySet()) {
                        String k = entry.getKey();
                        List<String> v = entry.getValue();

                        for (String s : v) {
                            if (s.contains(finalGroupId + ":" + finalArtifactId + " " + finalVersion)) {
                                if (result.containsKey(k)) {
                                    result.get(k).add(s);
                                } else {
                                    List<String> list = new ArrayList<>();
                                    list.add(s);
                                    result.put(k, list);
                                }
                                found = true;
                                licenseCount++;
                                break;
                            } else if (s.contains(finalArtifactId + " " + finalVersion)) {
                                if (result.containsKey(k)) {
                                    result.get(k).add(s);
                                } else {
                                    List<String> list = new ArrayList<>();
                                    list.add(s);
                                    result.put(k, list);
                                }
                                found = true;
                                licenseCount++;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        System.out.printf("%-80s %s%n", f.getName(), "LICENSE not found");
                    }
                    if (!existsNotice) {
                        System.out.printf("%-80s %s%n", f.getName(), "NOTICE  not found");
                    }
                }
                System.out.println("licenseCount:" + licenseCount + " fileCount:" + fileCount);
                System.out.println("noticeCount:" + noticeCount + " fileCount:" + fileCount);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
