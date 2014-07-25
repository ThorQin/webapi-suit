/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi.smc;

import java.awt.Dialog;
import java.util.List;
import javax.swing.JOptionPane;
import org.thordev.webapi.security.Security;
import org.thordev.webapi.security.SecuritySetting.Rule;
import static org.thordev.webapi.security.SecuritySetting.RuleAction.allow;
import static org.thordev.webapi.security.SecuritySetting.RuleAction.check_db;
import static org.thordev.webapi.security.SecuritySetting.RuleAction.deny;

/**
 *
 * @author nuo.qin
 */
public class RuleDialog extends javax.swing.JDialog {

	private Rule rule;
	private List<Rule> ruleList;
	private boolean ok = false;
	
	public RuleDialog(Dialog parent, boolean modal) {
		super(parent, modal);
		initComponents();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanel3 = new javax.swing.JPanel();
        textUser = new javax.swing.JTextField();
        textOperation = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        textScenario = new javax.swing.JTextField();
        textRole = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        textResourceType = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        textResourceId = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        textRuleName = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        textDescription = new javax.swing.JTextField();
        radioAllow = new javax.swing.JRadioButton();
        radioDeny = new javax.swing.JRadioButton();
        jLabel6 = new javax.swing.JLabel();
        radioDB = new javax.swing.JRadioButton();
        jSeparator2 = new javax.swing.JSeparator();
        buttonCancel = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Rule");
        setModalityType(java.awt.Dialog.ModalityType.DOCUMENT_MODAL);
        setResizable(false);
        setType(java.awt.Window.Type.POPUP);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jLabel11.setText("Scenario");

        jLabel7.setText("Role");
        jLabel7.setToolTipText("");

        jLabel13.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/tooltip.png"))); // NOI18N
        jLabel13.setToolTipText("<html>Can specify multiple roles, use ',' to split items.<br> <span style='color:red'>If keep empty that means no role will be matched, <br>specify '*' means all roles will be matched.</span></html>");

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jLabel1.setText("Resource Type (must)");

        jLabel14.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/tooltip.png"))); // NOI18N
        jLabel14.setToolTipText("<html>Can specify multiple users, use ',' to split items. <br><span style='color:red'>If keep empty that means no one will be matched,<br>specify '*' means all users will be matched.</span></html>");

        jLabel15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/tooltip.png"))); // NOI18N
        jLabel15.setToolTipText("<html>Specify '*' means will match all resources and resource id will be ignored. <br>Does not support multiple resource types. </html>");

        jLabel16.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/tooltip.png"))); // NOI18N
        jLabel16.setToolTipText("<html>Can specify multiple IDs, use ',' to split items. <br>If keep empty that means rule will match all instances of the resource.</html>");

        jLabel2.setText("Resource IDs");

        jLabel17.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/tooltip.png"))); // NOI18N
        jLabel17.setToolTipText("<html>Can specify multiple operations, use ',' to split items. <br>If keep empty that means apply to all operations.</html>");

        jLabel18.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/tooltip.png"))); // NOI18N
        jLabel18.setToolTipText("<html>Can specify multiple scenarios, use ',' to split items. <br>If keep empty that means the rule will apply to all scenarios.</html>");

        jLabel9.setText("User");

        jLabel5.setText("Operations");

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jLabel3.setText("Rule Name");

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jLabel4.setText("Description");

        buttonGroup1.add(radioAllow);
        radioAllow.setSelected(true);
        radioAllow.setText("Allow");
        radioAllow.setOpaque(false);

        buttonGroup1.add(radioDeny);
        radioDeny.setText("Deny");
        radioDeny.setOpaque(false);

        jLabel6.setText("Perform Action: ");

        buttonGroup1.add(radioDB);
        radioDB.setText("Check In DB");
        radioDB.setOpaque(false);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(textDescription, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jLabel4)
                                .addGroup(jPanel3Layout.createSequentialGroup()
                                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel15))
                                .addGroup(jPanel3Layout.createSequentialGroup()
                                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(jLabel13))
                                .addGroup(jPanel3Layout.createSequentialGroup()
                                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel17))
                                .addComponent(textRuleName, javax.swing.GroupLayout.DEFAULT_SIZE, 216, Short.MAX_VALUE)
                                .addComponent(textRole)
                                .addComponent(textResourceType)
                                .addComponent(textOperation))
                            .addComponent(jLabel3))
                        .addGap(18, 18, Short.MAX_VALUE)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(textUser, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel3Layout.createSequentialGroup()
                                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel16))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel3Layout.createSequentialGroup()
                                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel14))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel3Layout.createSequentialGroup()
                                    .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel18))
                                .addComponent(textResourceId)
                                .addComponent(textScenario))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(radioAllow, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(radioDeny, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radioDB)))
                        .addGap(18, 18, 18)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textRuleName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(radioAllow)
                    .addComponent(radioDeny)
                    .addComponent(radioDB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textDescription, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel16))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textResourceId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel9)
                            .addComponent(jLabel14))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel11)
                            .addComponent(jLabel18))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textScenario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel15))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textResourceType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel13))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textRole, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel5)
                            .addComponent(jLabel17))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textOperation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(26, Short.MAX_VALUE))
        );

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonOK.setText("OK");
        buttonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOKActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator2)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonOK, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(buttonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(36, 36, 36))
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                .addContainerGap(22, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

	
	public boolean isOk() {
		return ok;
	}
	
    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
		ok = false;
		if (rule == null)
		   return;
		textRuleName.setText(rule.name);
		textDescription.setText(rule.description);
		textResourceType.setText(rule.resType);
		textResourceId.setText(Security.join(rule.resId));
		textOperation.setText(Security.join(rule.operation));
		textRole.setText(Security.join(rule.role));
		textUser.setText(Security.join(rule.user));
		textScenario.setText(Security.join(rule.scenario));
		if (rule.action == allow) {
			radioAllow.setSelected(true);
		} else if (rule.action == deny) {
			radioDeny.setSelected(true);
		} else
			radioDB.setSelected(true);
    }//GEN-LAST:event_formWindowOpened

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
		this.dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        if (textRuleName.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(null, 
					"Must specify rule name!", 
					"Error", JOptionPane.WARNING_MESSAGE);
			textRuleName.requestFocus(true);
			return;
		}
		if (textDescription.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(null, 
					"Must provide rule description!", 
					"Error", JOptionPane.WARNING_MESSAGE);
			textDescription.requestFocus(true);
			return;
		}
		if (textResourceType.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(null, 
					"Must provide resource type!", 
					"Error", JOptionPane.WARNING_MESSAGE);
			textResourceType.requestFocus(true);
			return;
		}
		if (textRole.getText().trim().isEmpty() && textUser.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(null, 
					"Must specify either an user or a role, specify '*' to match all!", 
					"Error", JOptionPane.WARNING_MESSAGE);
			textResourceType.requestFocus(true);
			return;
		}
		String name = textRuleName.getText().trim();
		if (rule == null) {
			for (Rule r : ruleList) {
				if (r.name.equals(name)) {
					JOptionPane.showMessageDialog(null, 
						"Must specify unique rule name!", 
						"Error", JOptionPane.WARNING_MESSAGE);
						textRuleName.requestFocus(true);
					return;
				}
			}
		   rule = new Rule();
		} else {
			for (Rule r : ruleList) {
				if (!r.equals(rule) && r.name.equals(name)) {
					JOptionPane.showMessageDialog(null, 
						"Must specify unique rule name!", 
						"Error", JOptionPane.WARNING_MESSAGE);
						textRuleName.requestFocus(true);
					return;
				}
			}
		}
		rule.name = name;
		rule.action = radioAllow.isSelected() ? allow : (radioDeny.isSelected() ? deny : check_db);
		rule.description = textDescription.getText().trim();
		rule.resType = textResourceType.getText().trim();
		rule.resId = Security.split(textResourceId.getText());
		rule.operation = Security.split(textOperation.getText());
		rule.role = Security.split(textRole.getText());
		rule.user = Security.split(textUser.getText());
		rule.scenario = Security.split(textScenario.getText());
		ok = true;
		this.dispose();
    }//GEN-LAST:event_buttonOKActionPerformed

	public Rule getRule() {
		return rule;
	}
	
	public void setRule(Rule rule) {
		this.rule = rule;
	}
	
	public void setRuleList(List<Rule> ruleList) {
		this.ruleList = ruleList;
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton buttonOK;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JRadioButton radioAllow;
    private javax.swing.JRadioButton radioDB;
    private javax.swing.JRadioButton radioDeny;
    private javax.swing.JTextField textDescription;
    private javax.swing.JTextField textOperation;
    private javax.swing.JTextField textResourceId;
    private javax.swing.JTextField textResourceType;
    private javax.swing.JTextField textRole;
    private javax.swing.JTextField textRuleName;
    private javax.swing.JTextField textScenario;
    private javax.swing.JTextField textUser;
    // End of variables declaration//GEN-END:variables
}
