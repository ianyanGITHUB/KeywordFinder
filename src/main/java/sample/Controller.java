package sample;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sample.util.ThreadUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static sample.util.ThreadUtil.executor;
import static sample.util.ThreadUtil.listeningExecutor;

public class Controller {
    static final String NEED_HANDLE = "NEED_HANDLE";
    static volatile boolean flag = true;

    @FXML
    private Button submit;

    @FXML
    private Button selectBtn;

    @FXML
    private Button save;

    @FXML
    private Button interrupt;

    @FXML
    private Button reset;

    @FXML
    private TextField sourceText;

    @FXML
    private TextField keywords;

    @FXML
    private TextArea outcome;

    /**
     * 重置
     *
     * @param event
     */
    @FXML
    void reset(ActionEvent event) {
        keywords.setText("");
        sourceText.setText("");
        outcome.setText("");
    }

    /**
     * 查询关键字
     *
     * @param event
     */
    @FXML
    void submit(ActionEvent event) {
        flag = true;
        String keywordsText = keywords.getText();
        String directoryPath = sourceText.getText();
        if (isBlank(keywordsText) || isBlank(directoryPath)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("关键字或数据源输入为空!");
            alert.showAndWait();
            return;
        }
        outcome.setWrapText(true);
        Map<String, Set<String>> keyResultMap = new LinkedHashMap<>();
        if (keywordsText.contains("，") || keywordsText.contains(",")) {
            for (String s : keywordsText.trim().split("，|,")) {
                keyResultMap.put(s.trim(), new HashSet<>());
            }
        } else {
            keyResultMap.put(keywordsText.trim(), new HashSet<>());
        }

        ArrayList<String> listFileName = new ArrayList<>();
        getAllFileName(directoryPath, listFileName);

        if (listFileName.size() == 0) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("文件夹为空!");
            alert.showAndWait();
            return;
        } else {
            outcome.setText("");
            outcome.appendText("[信息] 本次查询关键字有" + keyResultMap.keySet().size() + "个：");
            for (String s : keyResultMap.keySet()) {
                outcome.appendText(s + " ");
            }
            outcome.appendText("\n");
            outcome.appendText("[信息] 本次查询路径为：" + directoryPath + "\n");
            outcome.appendText("[信息] 共扫描文件" + listFileName.size() + "个\n");
        }

        List<String> ableList = new ArrayList<>();
        Set<String> needHandle = new HashSet<>();
        for (String name : listFileName) {
            if (name.endsWith(".docx") || name.endsWith(".doc")) {
                ableList.add(name);
            } else {
                needHandle.add(name);
            }
        }
        keyResultMap.put(NEED_HANDLE, needHandle);
        //执行查询
        Map<String, Set<String>> resultMap = getSearchResult(keyResultMap, ableList);
        //输出结果
        outputResult(resultMap);
    }

    /**
     * 保存结果到文件
     *
     * @param event
     */
    @FXML
    void save(ActionEvent event) {
        if (isBlank(outcome.getText())) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setContentText("无结果导出!");
            alert.showAndWait();
            return;
        }
        Stage stage = (Stage) save.getScene().getWindow();
        FileChooser fc = new FileChooser();
        fc.setTitle("保存结果");
        fc.setInitialFileName("查询结果");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("文本类型", ".txt")
        );
        File file = fc.showSaveDialog(stage);
        if (file == null) return;
        FileOutputStream fos;
        OutputStreamWriter osw;
        try {
            fos = new FileOutputStream(file);
            osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            osw.write(outcome.getText());
            osw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("结果保存失败!");
            alert.showAndWait();
        }
    }

    /**
     * 选择数据源
     *
     * @param event
     */
    @FXML
    void selectDirectory(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("选择数据源");
        Stage stage = (Stage) selectBtn.getScene().getWindow();
        File files = dc.showDialog(stage);
        if (files != null) {
            sourceText.setText(files.getAbsolutePath());
        }
    }

    /**
     * 退出
     *
     * @param event
     */
    @FXML
    void exit(ActionEvent event) {
        flag = false;
        if (executor != null && executor.isShutdown()) {
            executor.shutdown();
        }
        Platform.exit();
    }

    /**
     * 获取所有文件路径
     *
     * @param path
     * @param listFileName
     */
    private void getAllFileName(String path, ArrayList<String> listFileName) {
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null || file.length() == 0) return;
        for (File a : files) {
            if (a.isDirectory()) {//如果文件夹下有子文件夹，获取子文件夹下的所有文件全路径。
                getAllFileName(a.getAbsolutePath(), listFileName);
            } else {
                listFileName.add(a.getAbsolutePath());
            }
        }
    }

    /**
     * 判断是否为空
     *
     * @param cs
     * @return
     */
    private boolean isBlank(CharSequence cs) {
        int strLen;
        if (cs != null && (strLen = cs.length()) != 0) {
            for (int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return true;
        }
    }

    /**
     * 执行查询
     *
     * @param keyResultMap
     * @param ableList
     * @return
     */
    private Map<String, Set<String>> getSearchResult(Map<String, Set<String>> keyResultMap, List<String> ableList) {
        Map<String, Set<String>> resultMap = new LinkedHashMap<>();
        List<Map<String, Set<String>>> resultList = new ArrayList<>();
        int size = ableList.size() / 20;
        if (size == 0) size = 10;
        List<List<String>> partition = Lists.partition(ableList, size);
        CountDownLatch latch = new CountDownLatch(partition.size());
        if (executor.isShutdown()) ThreadUtil.init();
        for (List<String> fileList : partition) {
            SearchKeywordTask searchKeywordTask = new SearchKeywordTask(keyResultMap, fileList, latch);
            Futures.addCallback(listeningExecutor.submit(searchKeywordTask), new FutureCallback<Map<String, Set<String>>>() {
                //成功时的回调方法
                @Override
                public void onSuccess(Map<String, Set<String>> result) {
                    resultList.add(result);
                }
                //失败时的回调方法
                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText(null);
                    alert.setContentText("系统正忙，请稍候重试!");
                    alert.showAndWait();
                }
            }, executor);
        }
        try {
            latch.await(3, TimeUnit.HOURS);//最多等待3小时结束
            executor.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("系统正忙，请稍候重试!");
            alert.showAndWait();
        }
        for (Map<String, Set<String>> map : resultList) {
            for (String s : map.keySet()) {
                resultMap.computeIfAbsent(s, k -> new HashSet<>());
                resultMap.get(s).addAll(map.get(s));
            }
        }
        return resultMap;
    }

    /**
     * 输出结果
     *
     * @param resultMap
     */
    private void outputResult(Map<String, Set<String>> resultMap) {
        Set<String> needHandle = resultMap.get(NEED_HANDLE);
        for (String keyword : resultMap.keySet()) {
            if (!NEED_HANDLE.equals(keyword)) {
                if (resultMap.get(keyword).size() > 0) {
                    outcome.appendText("\n");
                    outcome.appendText("[信息] 共有" + resultMap.get(keyword).size() + "个文件包含关键字【 " + keyword + " 】:\n");
                    for (String name : resultMap.get(keyword)) {
                        outcome.appendText(name + "\n");
                    }
                } else {
                    if (needHandle.size() == 0) {
                        outcome.appendText("\n");
                        outcome.appendText("[信息] 未发现包含关键字【 " + keyword + " 】的文件\n");
                    } else {
                        outcome.appendText("\n");
                        outcome.appendText("[信息] 暂未发现包含关键字【 " + keyword + " 】的文件，请在待处理的文件中人工查询\n");
                    }
                }
            }
        }
        if (needHandle != null && needHandle.size() > 0) {
            outcome.appendText("\n");
            outcome.appendText("[警告] 共有" + needHandle.size() + "个文件因技术问题需人工处理:\n");
            for (String name : needHandle) {
                outcome.appendText(name + "\n");
            }
        }
    }
}
