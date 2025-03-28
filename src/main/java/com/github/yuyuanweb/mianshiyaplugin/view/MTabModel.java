package com.github.yuyuanweb.mianshiyaplugin.view;

import javax.swing.table.DefaultTableModel;

/**
 * 禁止编辑的数据模型
 *
 * @author pine
 */
public class MTabModel extends DefaultTableModel {

    // 自定义表格模型，确保表格不可编辑
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

}
