package sample;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static sample.Controller.NEED_HANDLE;
import static sample.Controller.flag;

public class SearchKeywordTask implements Callable<Map<String, Set<String>>> {
    private List<String> fileList;
    private static ThreadLocal<Map<String, Set<String>>> resultMap;
    private CountDownLatch latch;

    SearchKeywordTask(Map<String, Set<String>> keyResultMap, List<String> fileList, CountDownLatch latch) {
        if (resultMap == null || !keyResultMap.equals(resultMap.get())) {
            resultMap = ThreadLocal.withInitial(() -> keyResultMap);
        }
        this.fileList = fileList;
        this.latch = latch;
    }

    @Override
    public Map<String, Set<String>> call() throws Exception {
        Map<String, Set<String>> localResultMap = resultMap.get();
        while (flag) {
            for (String name : fileList) {
                if (name.endsWith(".doc")) {
                    contextOfDoc(name, localResultMap);
                } else if (name.endsWith(".docx")) {
                    contextOfDocx(name, localResultMap);
                }
            }
            latch.countDown();
            if (flag) break;
        }
        if (!flag) Thread.currentThread().interrupt();
        return localResultMap;
    }

    /**
     * 从doc文件查询关键字
     *
     * @param name
     * @param localResultMap
     */
    private void contextOfDoc(String name, Map<String, Set<String>> localResultMap) {
        File file = new File(name);
        String subName = name.substring(name.lastIndexOf(File.separator), name.lastIndexOf("."));
        String str = "";
        try {
            FileInputStream fis = new FileInputStream(file);
            HWPFDocument doc = new HWPFDocument(fis);
            str = doc.getDocumentText();
            if (str != null) {
                String replace = str.replaceAll("\\s*", "");
                for (String keyword : localResultMap.keySet()) {
                    if (!NEED_HANDLE.equals(keyword) && (subName.contains(keyword) || replace.contains(keyword))) {
                        localResultMap.get(keyword).add(name);
                    }
                }
            }
            doc.close();
            fis.close();
        } catch (Exception e) {
            localResultMap.get(NEED_HANDLE).add(name);
        }
    }

    /**
     * 从docx文件查询关键字
     *
     * @param name
     * @param localResultMap
     */
    private void contextOfDocx(String name, Map<String, Set<String>> localResultMap) {
        File file = new File(name);
        String subName = name.substring(name.lastIndexOf(File.separator), name.lastIndexOf("."));
        String str = "";
        try {
            FileInputStream fis = new FileInputStream(file);
            XWPFDocument docx = new XWPFDocument(fis);
            XWPFWordExtractor extractor = new XWPFWordExtractor(docx);
            str = extractor.getText();
            if (str != null) {
                String replace = str.replaceAll("\\s*", "");
                for (String keyword : localResultMap.keySet()) {
                    if (!NEED_HANDLE.equals(keyword) && (subName.contains(keyword) || replace.contains(keyword))) {
                        localResultMap.get(keyword).add(name);
                    }
                }
            }
            extractor.close();
            fis.close();
        } catch (Exception e) {
            localResultMap.get(NEED_HANDLE).add(name);
        }
    }
}