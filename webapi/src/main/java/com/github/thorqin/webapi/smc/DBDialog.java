/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.thorqin.webapi.smc;

import java.awt.Dialog;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import com.github.thorqin.webapi.database.DBConfig.DBSetting.DBSettingItem;
import com.github.thorqin.webapi.utility.StringUtil;

/**
 *
 * @author nuo.qin
 */
public class DBDialog extends javax.swing.JDialog {
	public String key;
	public Set<String> keys;
	private DBSettingItem dbItem;
	public boolean ok = false;

	public DBDialog(Dialog parent, boolean modal) {
		super(parent, modal);
		initComponents();
	}

	public DBSettingItem getSetting() {
		return dbItem;
	}

	public void setSetting(DBSettingItem rule) {
		this.dbItem = rule;
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        textDBUri = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        textDBUser = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        textDBPassword = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        textDBDriver = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        textMinConnPerPartition = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        textMaxConnPerPartition = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        textPartitions = new javax.swing.JTextField();
        checkDefaultAutoCommit = new javax.swing.JCheckBox();
        textConfigName = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        buttonOK = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Database Setting");
        setModal(true);
        setModalityType(java.awt.Dialog.ModalityType.DOCUMENT_MODAL);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jPanel1.setOpaque(false);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jLabel1.setText("Database URI (must)");

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jLabel2.setText("Database User (must)");

        jLabel5.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jLabel5.setText("Database Password (must)");

        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jLabel7.setText("Database Driver (must)");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(textDBUri, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(textDBUser, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(textDBPassword)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(textDBDriver, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textDBDriver, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textDBUri, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textDBUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textDBPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel2.setOpaque(false);

        jLabel3.setText("Minimum Connections Per Partition");

        jLabel4.setText("Maximum Connections Per Partition");

        jLabel6.setText("Partitions Count");

        checkDefaultAutoCommit.setSelected(true);
        checkDefaultAutoCommit.setText("Default Auto Commit");
        checkDefaultAutoCommit.setOpaque(false);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(textMinConnPerPartition, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(textMaxConnPerPartition, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(textPartitions)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(checkDefaultAutoCommit)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textMinConnPerPartition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textMaxConnPerPartition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textPartitions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(checkDefaultAutoCommit)
                .addContainerGap(22, Short.MAX_VALUE))
        );

        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jLabel8.setText("Config Name");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(textConfigName, javax.swing.GroupLayout.PREFERRED_SIZE, 578, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(25, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(22, Short.MAX_VALUE)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textConfigName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(19, 19, 19)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        buttonOK.setText("OK");
        buttonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOKActionPerformed(evt);
            }
        });

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonOK, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(buttonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonOK))
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

	public static boolean isIntegerOrEmpty(JTextField text) {
		return StringUtil.isIntegerOrEmpty(text.getText().trim());
	}

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        if (textConfigName.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(null,
                "Must specify config name!",
                "Error", JOptionPane.WARNING_MESSAGE);
            textConfigName.requestFocus(true);
            return;
		}
		if (keys != null && keys.contains(textConfigName.getText().trim())) {
			JOptionPane.showMessageDialog(null,
                "Config name already exists!",
                "Error", JOptionPane.WARNING_MESSAGE);
            textConfigName.requestFocus(true);
            return;
		}
		
		if (textDBDriver.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "Must specify database driver!",
                "Error", JOptionPane.WARNING_MESSAGE);
            textDBDriver.requestFocus(true);
            return;
        }
		if (textDBDriver.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "Must specify database driver!",
                "Error", JOptionPane.WARNING_MESSAGE);
            textDBDriver.requestFocus(true);
            return;
        }
		if (textDBUri.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "Must specify database URI!",
                "Error", JOptionPane.WARNING_MESSAGE);
            textDBUri.requestFocus(true);
            return;
        }
		if (textDBUser.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "Must specify database user!",
                "Error", JOptionPane.WARNING_MESSAGE);
            textDBUser.requestFocus(true);
            return;
        }
		if (textDBPassword.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "Must specify database password!",
                "Error", JOptionPane.WARNING_MESSAGE);
            textDBPassword.requestFocus(true);
            return;
        }
		if (!isIntegerOrEmpty(textMinConnPerPartition)) {
			JOptionPane.showMessageDialog(null,
                "Must be an integer!",
                "Error", JOptionPane.WARNING_MESSAGE);
            textMinConnPerPartition.requestFocus(true);
            return;
		}
		if (!isIntegerOrEmpty(textMaxConnPerPartition)) {
			JOptionPane.showMessageDialog(null,
                "Must be an integer!",
                "Error", JOptionPane.WARNING_MESSAGE);
            textMaxConnPerPartition.requestFocus(true);
            return;
		}
		if (!isIntegerOrEmpty(textPartitions)) {
			JOptionPane.showMessageDialog(null,
                "Must be an integer!",
                "Error", JOptionPane.WARNING_MESSAGE);
            textPartitions.requestFocus(true);
            return;
		}
		key = textConfigName.getText();
		dbItem.driver = textDBDriver.getText().trim();
		dbItem.uri = textDBUri.getText().trim();
		dbItem.user = textDBUser.getText().trim();
		dbItem.password = textDBPassword.getText().trim();
        dbItem.minConnectionsPerPartition = Integer.parseInt(textMinConnPerPartition.getText().trim());
		dbItem.maxConnectionsPerPartition = Integer.parseInt(textMaxConnPerPartition.getText().trim());
		dbItem.partitionCount = Integer.parseInt(textPartitions.getText().trim());
		dbItem.defaultAutoCommit = checkDefaultAutoCommit.isSelected();
        ok = true;
        this.dispose();
    }//GEN-LAST:event_buttonOKActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        this.dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

	private String generateKey() {
		String name = "default";
		String newKey = name;
		int i = 1;
		while (keys.contains(newKey)) {
			newKey = name + i;
			i++;
		}
		return newKey;
	}
	
    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        if (dbItem == null) {
			dbItem = new DBSettingItem();
		}
		if (key == null) {
			textConfigName.setText(generateKey());
		} else {
			textConfigName.setText(key);
			textConfigName.setEnabled(false);
		}
		textDBUri.setText(dbItem.uri);
		textDBUser.setText(dbItem.user);
		textDBPassword.setText(dbItem.password);
		textDBDriver.setText(dbItem.driver);
		textMinConnPerPartition.setText(String.valueOf(dbItem.minConnectionsPerPartition));
		textMaxConnPerPartition.setText(String.valueOf(dbItem.maxConnectionsPerPartition));
		textPartitions.setText(String.valueOf(dbItem.partitionCount));
		checkDefaultAutoCommit.setSelected(dbItem.defaultAutoCommit);
    }//GEN-LAST:event_formWindowOpened


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOK;
    private javax.swing.JCheckBox checkDefaultAutoCommit;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTextField textConfigName;
    private javax.swing.JTextField textDBDriver;
    private javax.swing.JTextField textDBPassword;
    private javax.swing.JTextField textDBUri;
    private javax.swing.JTextField textDBUser;
    private javax.swing.JTextField textMaxConnPerPartition;
    private javax.swing.JTextField textMinConnPerPartition;
    private javax.swing.JTextField textPartitions;
    // End of variables declaration//GEN-END:variables
}
