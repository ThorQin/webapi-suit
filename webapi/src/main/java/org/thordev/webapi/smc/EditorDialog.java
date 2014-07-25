/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi.smc;

import java.awt.Component;
import java.io.File;
import static java.lang.System.exit;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.thordev.webapi.amq.AMQConfig.AMQSetting.AMQSettingItem;
import org.thordev.webapi.database.DBConfig.DBSetting.DBSettingItem;
import org.thordev.webapi.mail.MailConfig.MailSetting.MailSettingItem;
import org.thordev.webapi.security.SecuritySetting;
import org.thordev.webapi.security.SecuritySetting.Rule;
import static org.thordev.webapi.security.SecuritySetting.RuleAction.allow;
import static org.thordev.webapi.security.SecuritySetting.RuleAction.deny;
import org.thordev.webapi.security.SecuritySetting.URLMatcher;


class MatcherTableModel extends AbstractTableModel  {

	private final String[] columns;
	private final List<URLMatcher> matchers;
	public MatcherTableModel(List<URLMatcher> matchers) {
		this.matchers = matchers;
		this.columns = new String [] {
			"Matcher Name", "Description", "URL"
		};
		
	}
	
	@Override
	public int getRowCount() {
		return matchers.size();
	}

	@Override
	public int getColumnCount() {
		return columns.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		URLMatcher matcher = matchers.get(rowIndex);
		switch (columnIndex) {
			case 0:
				return matcher.name;
			case 1:
				return matcher.description;
			case 2:
				return matcher.url;
		}
		return null;
	}
	
	@Override
	public String getColumnName(int col) {
		return columns[col];
    }
}

class TextAndIcon {
	public String text;
	public ImageIcon icon;
}

class ImageRenderer extends DefaultTableCellRenderer {

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); //To change body of generated methods, choose Tools | Templates.
		if (column == 0) {
			JLabel label = (JLabel)component;
			TextAndIcon info = (TextAndIcon)value;
			label.setIcon(info.icon);
			label.setText(info.text);
		}
		return component;
	}	
}

class RuleTableModel extends AbstractTableModel  {

	private final String[] columns;
	private final List<Rule> rules;
	public RuleTableModel(List<Rule> rules) {
		this.rules = rules;
		this.columns = new String [] {
			"Rule Name", "Description", "Action"
		};
	}
	
	@Override
	public int getRowCount() {
		return rules.size();
	}

	@Override
	public int getColumnCount() {
		return columns.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Rule rule = rules.get(rowIndex);
		switch (columnIndex) {
			case 0:
				TextAndIcon label = new TextAndIcon();
				label.text = rule.name;
				if (rule.action == allow) {
					label.icon = new ImageIcon(getClass().getResource("allow.png"));
				} else if (rule.action == deny) {
					label.icon = new ImageIcon(getClass().getResource("deny.png"));
				} else {
					label.icon = new ImageIcon(getClass().getResource("advance.png"));
				}
				return label;
			case 1:
				return rule.description;
			case 2:
				if (rule.action == allow) {
					return "Allow";
				} else if (rule.action == deny) {
					return "Deny";
				} else {
					return "Check In Database";
				}
		}
		return null;
	}
	@Override
	public String getColumnName(int col) {
		return columns[col];
    }
}

class DBModel extends MapTableModelBase<DBSettingItem> {
	public DBModel(Map<String, DBSettingItem> items) {
		super(items, new String[]{"Name", "Driver", "Uri", "User", "Password"});
	}

	@Override
	protected String getColValue(int columnIndex, Map.Entry<String, DBSettingItem> entry) {
		switch (columnIndex) {
			case 0:
				return entry.getKey();
			case 1:
				return entry.getValue().driver;
			case 2:
				return entry.getValue().uri;
			case 3:
				return entry.getValue().user;
			case 4:
				return entry.getValue().password;
			default:
				return null;
		}
	}
}

class AMQModel extends MapTableModelBase<AMQSettingItem> {
	public AMQModel(Map<String, AMQSettingItem> items) {
		super(items, new String[]{"Name", "Uri", "User", "Password", "Default Address"});
	}

	@Override
	protected String getColValue(int columnIndex, Map.Entry<String, AMQSettingItem> entry) {
		switch (columnIndex) {
			case 0:
				return entry.getKey();
			case 1:
				return entry.getValue().uri;
			case 2:
				return entry.getValue().user;
			case 3:
				return entry.getValue().password;
			case 4:
				return entry.getValue().address;
			default:
				return null;
		}
	}
}

class MailModel extends MapTableModelBase<MailSettingItem> {
	public MailModel(Map<String, MailSettingItem> items) {
		super(items, new String[]{"Name", "Host", "Port", "User", "Password", "Mail From"});
	}

	@Override
	protected String getColValue(int columnIndex, Map.Entry<String, MailSettingItem> entry) {
		switch (columnIndex) {
			case 0:
				return entry.getKey();
			case 1:
				return entry.getValue().host;
			case 2:
				return String.valueOf(entry.getValue().port);
			case 3:
				return entry.getValue().user;
			case 4:
				return entry.getValue().password;
			case 5:
				return entry.getValue().from;
			default:
				return null;
		}
	}
}
/**
 *
 * @author nuo.qin
 */
public class EditorDialog extends javax.swing.JDialog {
	private WebConfig config;
	private DBModel dbModel;
	private AMQModel amqModel;
	private MailModel mailModel;
	private RuleTableModel ruleModel;
	private MatcherTableModel matcherModel;
	private String configFile = null;
	
	public EditorDialog(java.awt.Frame parent) {
		super(parent, ModalityType.TOOLKIT_MODAL);
		initComponents();
	}
    
	public WebConfig getConfig() {
		return config;
	}

	public void setConfig(WebConfig config) {
		this.config = config;
	}
	
	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}
	
	private void syncDBCombo() {
		Object v = textDBConfig.getEditor().getItem();
		textDBConfig.removeAllItems();
		for (String k : config.getDB().keySet()) {
			textDBConfig.addItem(k);
		}
		textDBConfig.getEditor().setItem(v);
	}
	
	private void syncAMQCombo() {
		Object v = textAMQConfig.getEditor().getItem();
		textAMQConfig.removeAllItems();
		for (String k : config.getAmq().keySet()) {
			textAMQConfig.addItem(k);
		}
		textAMQConfig.getEditor().setItem(v);
	}
	
	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonOK = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        buttonAddDB = new javax.swing.JButton();
        buttonDeleteDB = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        tableDB = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        buttonAddAMQ = new javax.swing.JButton();
        buttonDeleteAMQ = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        tableAMQ = new javax.swing.JTable();
        jLabel7 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        buttonAddMail = new javax.swing.JButton();
        buttonDeleteMail = new javax.swing.JButton();
        jScrollPane6 = new javax.swing.JScrollPane();
        tableMail = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        panelProperties = new javax.swing.JTabbedPane();
        panelCommon = new javax.swing.JPanel();
        textSessionTimeout = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel15 = new javax.swing.JLabel();
        comboSessionType = new javax.swing.JComboBox();
        comboDefaultAction = new javax.swing.JComboBox();
        jLabel18 = new javax.swing.JLabel();
        textAMQConfig = new javax.swing.JComboBox();
        textDBConfig = new javax.swing.JComboBox();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        panelAllow = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableAllowRules = new javax.swing.JTable();
        buttonAddAllowRule = new javax.swing.JButton();
        buttonDeleteAllowRules = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        buttonRuleTop = new javax.swing.JButton();
        buttonRuleUp = new javax.swing.JButton();
        buttonRuleDown = new javax.swing.JButton();
        buttonRuleBottom = new javax.swing.JButton();
        panelURLMatcher = new javax.swing.JPanel();
        buttonAddMatcher = new javax.swing.JButton();
        buttonDeleteMatcher = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        tableMatchers = new javax.swing.JTable();
        jLabel11 = new javax.swing.JLabel();
        buttonMatcherTop = new javax.swing.JButton();
        buttonMatcherUp = new javax.swing.JButton();
        buttonMatcherDown = new javax.swing.JButton();
        buttonMatcherBottom = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Web Configuration Editor");
        setModalityType(java.awt.Dialog.ModalityType.DOCUMENT_MODAL);
        setName("editorDialog"); // NOI18N
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        buttonOK.setText("Save");
        buttonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOKActionPerformed(evt);
            }
        });

        buttonCancel.setText("Close");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        jLabel3.setBackground(new java.awt.Color(0, 102, 153));
        jLabel3.setFont(new java.awt.Font("Arial Black", 0, 24)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Web Configuration Editor");
        jLabel3.setOpaque(true);

        jPanel2.setOpaque(false);

        buttonAddDB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/add.png"))); // NOI18N
        buttonAddDB.setText("Add Database");
        buttonAddDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddDBActionPerformed(evt);
            }
        });

        buttonDeleteDB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/delete.png"))); // NOI18N
        buttonDeleteDB.setText("Delete");
        buttonDeleteDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteDBActionPerformed(evt);
            }
        });

        tableDB.setFont(tableDB.getFont().deriveFont(tableDB.getFont().getSize()+1f));
        tableDB.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Database Name", "URI", "Database User"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tableDB.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableDBMouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(tableDB);

        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/db.png"))); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 575, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonAddDB)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonDeleteDB)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonAddDB)
                            .addComponent(buttonDeleteDB))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(42, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Database", jPanel2);

        jPanel3.setOpaque(false);

        buttonAddAMQ.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/add.png"))); // NOI18N
        buttonAddAMQ.setText("Add ActiveMQ");
        buttonAddAMQ.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddAMQActionPerformed(evt);
            }
        });

        buttonDeleteAMQ.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/delete.png"))); // NOI18N
        buttonDeleteAMQ.setText("Delete");
        buttonDeleteAMQ.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteAMQActionPerformed(evt);
            }
        });

        tableAMQ.setFont(tableAMQ.getFont().deriveFont(tableAMQ.getFont().getSize()+1f));
        tableAMQ.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ActiveMQ Name", "URI", "ActiveMQ User"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tableAMQ.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableAMQMouseClicked(evt);
            }
        });
        jScrollPane5.setViewportView(tableAMQ);

        jLabel7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/im.png"))); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 575, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonAddAMQ)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonDeleteAMQ)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonAddAMQ)
                            .addComponent(buttonDeleteAMQ)))
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(42, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("ActiveMQ", jPanel3);

        jPanel5.setOpaque(false);

        jLabel19.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/amq.png"))); // NOI18N

        buttonAddMail.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/add.png"))); // NOI18N
        buttonAddMail.setText("Add SMTP Config");
        buttonAddMail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddMailActionPerformed(evt);
            }
        });

        buttonDeleteMail.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/delete.png"))); // NOI18N
        buttonDeleteMail.setText("Delete");
        buttonDeleteMail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteMailActionPerformed(evt);
            }
        });

        tableMail.setFont(tableMail.getFont().deriveFont(tableMail.getFont().getSize()+1f));
        tableMail.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Host", "User", "Password"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tableMail.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableMailMouseClicked(evt);
            }
        });
        jScrollPane6.setViewportView(tableMail);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 575, Short.MAX_VALUE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addComponent(jLabel19)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonAddMail)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonDeleteMail)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel19)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonAddMail)
                            .addComponent(buttonDeleteMail))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(42, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Mail Service", jPanel5);

        jPanel4.setOpaque(false);

        panelCommon.setBackground(new java.awt.Color(255, 255, 255));

        textSessionTimeout.setName(""); // NOI18N

        jLabel4.setText("Session Timeout");

        jLabel1.setText("Database Config");

        jLabel2.setText("ActiveMQ Config");

        jLabel15.setText("Session Type");

        comboSessionType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Client Side Session", "Server Side Session" }));

        comboDefaultAction.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Allow", "Deny" }));

        jLabel18.setText("Default Action");

        textAMQConfig.setEditable(true);

        textDBConfig.setEditable(true);

        jLabel16.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/tooltip.png"))); // NOI18N
        jLabel16.setToolTipText("<html>Provide database configuration name to store the security setting <br>\nin database and let security module can check privilege from database<br> \nwhen matched an advanced rule.</html>");

        jLabel17.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/tooltip.png"))); // NOI18N
        jLabel17.setToolTipText("<html>If you want to notify all front-side load balance server to obtain newly changed settings, \n<br>then you should specify ActiveMQ configuration name, when save this setting<br>\nthis tool will send a notification to all server which listen to the same ActiveMQ.</html>");

        javax.swing.GroupLayout panelCommonLayout = new javax.swing.GroupLayout(panelCommon);
        panelCommon.setLayout(panelCommonLayout);
        panelCommonLayout.setHorizontalGroup(
            panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCommonLayout.createSequentialGroup()
                .addGap(81, 81, 81)
                .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jSeparator1)
                    .addGroup(panelCommonLayout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel17))
                    .addGroup(panelCommonLayout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel16))
                    .addComponent(textAMQConfig, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(textDBConfig, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelCommonLayout.createSequentialGroup()
                        .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(textSessionTimeout)
                            .addComponent(comboSessionType, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(comboDefaultAction, javax.swing.GroupLayout.PREFERRED_SIZE, 262, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(87, Short.MAX_VALUE))
        );
        panelCommonLayout.setVerticalGroup(
            panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCommonLayout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(textSessionTimeout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboSessionType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(comboDefaultAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(32, 32, 32)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel16))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textDBConfig, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel17))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(textAMQConfig, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(28, Short.MAX_VALUE))
        );

        panelProperties.addTab("Common", panelCommon);

        panelAllow.setBackground(new java.awt.Color(255, 255, 255));

        tableAllowRules.setFont(tableAllowRules.getFont().deriveFont(tableAllowRules.getFont().getSize()+1f));
        tableAllowRules.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", "Rule Name", "Description", "Action"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tableAllowRules.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableAllowRulesMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tableAllowRules);

        buttonAddAllowRule.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/add.png"))); // NOI18N
        buttonAddAllowRule.setText("Add Rule");
        buttonAddAllowRule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddAllowRuleActionPerformed(evt);
            }
        });

        buttonDeleteAllowRules.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/delete.png"))); // NOI18N
        buttonDeleteAllowRules.setText("Delete");
        buttonDeleteAllowRules.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteAllowRulesActionPerformed(evt);
            }
        });

        jLabel8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/rule.png"))); // NOI18N

        buttonRuleTop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/top.png"))); // NOI18N
        buttonRuleTop.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        buttonRuleTop.setIconTextGap(0);
        buttonRuleTop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRuleTopActionPerformed(evt);
            }
        });

        buttonRuleUp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/up.png"))); // NOI18N
        buttonRuleUp.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        buttonRuleUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRuleUpActionPerformed(evt);
            }
        });

        buttonRuleDown.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/down.png"))); // NOI18N
        buttonRuleDown.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        buttonRuleDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRuleDownActionPerformed(evt);
            }
        });

        buttonRuleBottom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/bottom.png"))); // NOI18N
        buttonRuleBottom.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        buttonRuleBottom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRuleBottomActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelAllowLayout = new javax.swing.GroupLayout(panelAllow);
        panelAllow.setLayout(panelAllowLayout);
        panelAllowLayout.setHorizontalGroup(
            panelAllowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAllowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelAllowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelAllowLayout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonAddAllowRule)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonDeleteAllowRules))
                    .addGroup(panelAllowLayout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 496, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(panelAllowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(buttonRuleDown, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(buttonRuleUp, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(buttonRuleTop, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(buttonRuleBottom, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        panelAllowLayout.setVerticalGroup(
            panelAllowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAllowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelAllowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel8)
                    .addGroup(panelAllowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(buttonAddAllowRule)
                        .addComponent(buttonDeleteAllowRules)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelAllowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelAllowLayout.createSequentialGroup()
                        .addComponent(buttonRuleTop, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRuleUp, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRuleDown, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRuleBottom, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(36, Short.MAX_VALUE))
        );

        panelProperties.addTab("Security Rule", panelAllow);

        panelURLMatcher.setBackground(new java.awt.Color(255, 255, 255));

        buttonAddMatcher.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/add.png"))); // NOI18N
        buttonAddMatcher.setText("Add Matcher");
        buttonAddMatcher.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddMatcherActionPerformed(evt);
            }
        });

        buttonDeleteMatcher.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/delete.png"))); // NOI18N
        buttonDeleteMatcher.setText("Delete");
        buttonDeleteMatcher.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteMatcherActionPerformed(evt);
            }
        });

        tableMatchers.setFont(tableMatchers.getFont().deriveFont(tableMatchers.getFont().getSize()+1f));
        tableMatchers.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Description", "URL"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tableMatchers.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableMatchersMouseClicked(evt);
            }
        });
        jScrollPane4.setViewportView(tableMatchers);

        jLabel11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/address.png"))); // NOI18N

        buttonMatcherTop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/top.png"))); // NOI18N
        buttonMatcherTop.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        buttonMatcherTop.setIconTextGap(0);
        buttonMatcherTop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMatcherTopActionPerformed(evt);
            }
        });

        buttonMatcherUp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/up.png"))); // NOI18N
        buttonMatcherUp.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        buttonMatcherUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMatcherUpActionPerformed(evt);
            }
        });

        buttonMatcherDown.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/down.png"))); // NOI18N
        buttonMatcherDown.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        buttonMatcherDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMatcherDownActionPerformed(evt);
            }
        });

        buttonMatcherBottom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/bottom.png"))); // NOI18N
        buttonMatcherBottom.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        buttonMatcherBottom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMatcherBottomActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelURLMatcherLayout = new javax.swing.GroupLayout(panelURLMatcher);
        panelURLMatcher.setLayout(panelURLMatcherLayout);
        panelURLMatcherLayout.setHorizontalGroup(
            panelURLMatcherLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelURLMatcherLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelURLMatcherLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelURLMatcherLayout.createSequentialGroup()
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 496, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(panelURLMatcherLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(buttonMatcherDown, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(buttonMatcherUp, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(buttonMatcherTop, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(buttonMatcherBottom, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(panelURLMatcherLayout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonAddMatcher)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonDeleteMatcher)))
                .addContainerGap())
        );
        panelURLMatcherLayout.setVerticalGroup(
            panelURLMatcherLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelURLMatcherLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelURLMatcherLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel11)
                    .addGroup(panelURLMatcherLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(buttonAddMatcher)
                        .addComponent(buttonDeleteMatcher)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelURLMatcherLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(panelURLMatcherLayout.createSequentialGroup()
                        .addComponent(buttonMatcherTop, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonMatcherUp, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonMatcherDown, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonMatcherBottom, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(92, 92, 92))
        );

        panelProperties.addTab("URL Matcher", panelURLMatcher);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelProperties)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelProperties, javax.swing.GroupLayout.PREFERRED_SIZE, 333, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Security Setting", jPanel4);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonOK, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(buttonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 604, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 389, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonOK))
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        try {
			if (configFile != null) {
				try {
					config = new WebConfig(configFile);
				} catch (Exception ex) {
					config = new WebConfig();
				}
			} else {
				config = new WebConfig();
			}
		} catch (Exception ex) {
			System.out.println("Cannot construct configuration instance.");
			exit(-1);
		}
		this.dbModel = new DBModel(config.getDB());
		tableDB.setModel(dbModel);
		this.amqModel = new AMQModel(config.getAmq());
		tableAMQ.setModel(amqModel);
		this.mailModel = new MailModel(config.getMail());
		tableMail.setModel(mailModel);
		this.ruleModel = new RuleTableModel(config.getSecurity().rules);
		tableAllowRules.setModel(ruleModel);
		TableColumnModel columnModel = tableAllowRules.getColumnModel();
		columnModel.getColumn(0).setCellRenderer(new ImageRenderer());
		this.matcherModel = new MatcherTableModel(config.getSecurity().matchers);
		tableMatchers.setModel(matcherModel);
		textSessionTimeout.setText(String.valueOf(config.getSecurity().sessionTimeout));
		if (config.getSecurity().defaultAllow)
			comboDefaultAction.setSelectedIndex(0);
		else
			comboDefaultAction.setSelectedIndex(1);
		if (config.getSecurity().clientSession)
			comboSessionType.setSelectedIndex(0);
		else
			comboSessionType.setSelectedIndex(1);
		
	
		textDBConfig.getEditor().setItem(config.getSecurity().dbConfig);
		textAMQConfig.getEditor().setItem(config.getSecurity().amqConfig);
    }//GEN-LAST:event_formWindowOpened

	
    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        this.dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
		SecuritySetting securitySetting = config.getSecurity();
		try {
			securitySetting.sessionTimeout = Integer.parseInt(textSessionTimeout.getText());
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(null, "'Session Time Out' must be an positive integer.", "Error", JOptionPane.ERROR_MESSAGE);
			textSessionTimeout.requestFocus();
			return;
		}
		securitySetting.defaultAllow = (comboDefaultAction.getSelectedIndex() == 0);
		securitySetting.clientSession = (comboSessionType.getSelectedIndex() == 0);
		if (((String)textDBConfig.getEditor().getItem()).trim().isEmpty())
			securitySetting.dbConfig = null;
		else
			securitySetting.dbConfig = ((String)textDBConfig.getEditor().getItem()).trim();
		if (((String)textAMQConfig.getEditor().getItem()).trim().isEmpty())
			securitySetting.amqConfig = null;
		else
			securitySetting.amqConfig = ((String)textAMQConfig.getEditor().getItem()).trim();
		
		String file;
		if (configFile == null) {
			String path = EditorDialog.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			File dir = new File(path);
			JFileChooser fileChooser = new JFileChooser();
			FileExtensionFilter filter = new FileExtensionFilter("Web Config", "config");
			fileChooser.setFileFilter(filter);
			fileChooser.setCurrentDirectory(dir);
			int result = fileChooser.showSaveDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				file = fileChooser.getSelectedFile().getAbsolutePath();
				if (fileChooser.getFileFilter().getDescription().equals("Web Config")) {
					if (!file.toLowerCase().endsWith(".config")) {
						file += ".config";
					}
				}
				if (new File(file).exists()) {
					result = JOptionPane.showConfirmDialog(null, 
							"File already exists, would you want to override exist one?", 
							"Save File", JOptionPane.OK_CANCEL_OPTION);
					if (result != JOptionPane.OK_OPTION)
						return;
				}
			} else
				return;
		} else
			file = configFile;
		config.save(file);
		
		this.dispose();
    }//GEN-LAST:event_buttonOKActionPerformed

    private void buttonAddDBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddDBActionPerformed
        DBDialog dialog = new DBDialog(this, true);
		dialog.keys = config.getDB().keySet();
		dialog.setVisible(true);
		DBSettingItem item = dialog.getSetting();
		if (item != null && dialog.ok) {
			config.getDB().put(dialog.key, item);
			dbModel.fireTableDataChanged();
			int index = dbModel.getKeyIndex(dialog.key);
			tableDB.setRowSelectionInterval(index, index);
			syncDBCombo();
		}
    }//GEN-LAST:event_buttonAddDBActionPerformed

    private void buttonDeleteDBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteDBActionPerformed
        List<String> removed = new LinkedList<>();
		for (int rownum : tableDB.getSelectedRows()) {
			removed.add(dbModel.get(rownum).getKey());
		}
		for (String key: removed)
			config.getDB().remove(key);
		dbModel.fireTableDataChanged();
		syncDBCombo();
    }//GEN-LAST:event_buttonDeleteDBActionPerformed

    private void tableDBMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableDBMouseClicked
        if (evt.getClickCount() == 2) {
			int rowId = tableDB.getSelectedRow();
			Map.Entry<String, DBSettingItem> dbItem = dbModel.get(rowId);
			DBDialog dialog = new DBDialog(this, true);
			dialog.key = dbItem.getKey();
			dialog.setSetting(dbItem.getValue());
			dialog.setVisible(true);
			if (dialog.ok) {
				dbModel.fireTableDataChanged();
				tableDB.setRowSelectionInterval(rowId, rowId);
			}
		}
    }//GEN-LAST:event_tableDBMouseClicked

    private void buttonAddAMQActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddAMQActionPerformed
        AMQDialog dialog = new AMQDialog(this, true);
		dialog.keys = config.getAmq().keySet();
		dialog.setVisible(true);
		AMQSettingItem item = dialog.getSetting();
		if (item != null && dialog.ok) {
			config.getAmq().put(dialog.key, item);
			amqModel.fireTableDataChanged();
			syncAMQCombo();
			int index = amqModel.getKeyIndex(dialog.key);
			tableAMQ.setRowSelectionInterval(index, index);
		}
    }//GEN-LAST:event_buttonAddAMQActionPerformed

    private void buttonDeleteAMQActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteAMQActionPerformed
        List<String> removed = new LinkedList<>();
		for (int rownum : tableAMQ.getSelectedRows()) {
			removed.add(amqModel.get(rownum).getKey());
		}
		for (String key: removed)
			config.getAmq().remove(key);
		amqModel.fireTableDataChanged();
		syncAMQCombo();
    }//GEN-LAST:event_buttonDeleteAMQActionPerformed

    private void tableAMQMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableAMQMouseClicked
        if (evt.getClickCount() == 2) {
			int rowId = tableAMQ.getSelectedRow();
			Map.Entry<String, AMQSettingItem> amqItem = amqModel.get(rowId);
			AMQDialog dialog = new AMQDialog(this, true);
			dialog.key = amqItem.getKey();
			dialog.setSetting(amqItem.getValue());
			dialog.setVisible(true);
			if (dialog.ok) {
				amqModel.fireTableDataChanged();
				tableAMQ.setRowSelectionInterval(rowId, rowId);
			}
		}
    }//GEN-LAST:event_tableAMQMouseClicked

    private void buttonAddMailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddMailActionPerformed
        MailDialog dialog = new MailDialog(this, true);
		dialog.keys = config.getMail().keySet();
		dialog.setVisible(true);
		MailSettingItem item = dialog.getSetting();
		if (item != null && dialog.ok) {
			config.getMail().put(dialog.key, item);
			mailModel.fireTableDataChanged();
			int index = mailModel.getKeyIndex(dialog.key);
			tableMail.setRowSelectionInterval(index, index);
		}
    }//GEN-LAST:event_buttonAddMailActionPerformed

    private void buttonDeleteMailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteMailActionPerformed
        List<String> removed = new LinkedList<>();
		for (int rownum : tableMail.getSelectedRows()) {
			removed.add(mailModel.get(rownum).getKey());
		}
		for (String key: removed)
			config.getMail().remove(key);
		mailModel.fireTableDataChanged();
    }//GEN-LAST:event_buttonDeleteMailActionPerformed

    private void tableMailMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMailMouseClicked
        if (evt.getClickCount() == 2) {
			int rowId = tableMail.getSelectedRow();
			Map.Entry<String, MailSettingItem> mailItem = mailModel.get(rowId);
			MailDialog dialog = new MailDialog(this, true);
			dialog.key = mailItem.getKey();
			dialog.setSetting(mailItem.getValue());
			dialog.setVisible(true);
			if (dialog.ok) {
				mailModel.fireTableDataChanged();
				tableMail.setRowSelectionInterval(rowId, rowId);
			}
		}
    }//GEN-LAST:event_tableMailMouseClicked

    private void tableMatchersMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMatchersMouseClicked
        int rowId = tableMatchers.getSelectedRow();
        URLMatcher matcher = config.getSecurity().matchers.get(rowId);
        if (evt.getClickCount() == 2) {
            MatcherDialog dialog = new MatcherDialog(this, true);
            dialog.setMatcher(matcher);
			dialog.setMatcherList(config.getSecurity().matchers);
            dialog.setVisible(true);
            if (dialog.isOk()) {
                matcherModel.fireTableDataChanged();
                tableMatchers.setRowSelectionInterval(rowId, rowId);
            }
        }
    }//GEN-LAST:event_tableMatchersMouseClicked

    private void buttonDeleteMatcherActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteMatcherActionPerformed
        List<URLMatcher> removed = new LinkedList<>();
        for (int rownum : tableMatchers.getSelectedRows()) {
            removed.add(config.getSecurity().matchers.get(rownum));
        }
        config.getSecurity().matchers.removeAll(removed);
        matcherModel.fireTableDataChanged();
    }//GEN-LAST:event_buttonDeleteMatcherActionPerformed

    private void buttonAddMatcherActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddMatcherActionPerformed
        MatcherDialog dialog = new MatcherDialog(this, true);
		dialog.setMatcherList(config.getSecurity().matchers);
        dialog.setVisible(true);
        URLMatcher matcher = dialog.getMatcher();
        if (matcher != null) {
            config.getSecurity().matchers.add(matcher);
            matcherModel.fireTableDataChanged();
            tableMatchers.setRowSelectionInterval(config.getSecurity().matchers.size() - 1, config.getSecurity().matchers.size() - 1);
        }
    }//GEN-LAST:event_buttonAddMatcherActionPerformed

    private void buttonDeleteAllowRulesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteAllowRulesActionPerformed
        List<Rule> removed = new LinkedList<>();
        for (int rownum : tableAllowRules.getSelectedRows()) {
            removed.add(config.getSecurity().rules.get(rownum));
        }
        config.getSecurity().rules.removeAll(removed);
        ruleModel.fireTableDataChanged();
    }//GEN-LAST:event_buttonDeleteAllowRulesActionPerformed

    private void buttonAddAllowRuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddAllowRuleActionPerformed
        RuleDialog dialog = new RuleDialog(this, true);
		dialog.setRuleList(config.getSecurity().rules);
        dialog.setVisible(true);
        Rule rule = dialog.getRule();
        if (rule != null) {
            config.getSecurity().rules.add(rule);
            ruleModel.fireTableDataChanged();
            tableAllowRules.setRowSelectionInterval(config.getSecurity().rules.size() - 1, config.getSecurity().rules.size() - 1);
        }
    }//GEN-LAST:event_buttonAddAllowRuleActionPerformed

    private void tableAllowRulesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableAllowRulesMouseClicked
        if (evt.getClickCount() == 2) {
            int rowId = tableAllowRules.getSelectedRow();
            Rule rule = config.getSecurity().rules.get(rowId);
            RuleDialog dialog = new RuleDialog(this, true);
            dialog.setRule(rule);
			dialog.setRuleList(config.getSecurity().rules);
            dialog.setVisible(true);
            if (dialog.isOk()) {
                ruleModel.fireTableDataChanged();
                tableAllowRules.setRowSelectionInterval(rowId, rowId);
            }
        }
    }//GEN-LAST:event_tableAllowRulesMouseClicked

	private <T> void MoveItem(JTable table, List<T> collection, int pos) {
		int[] rows = table.getSelectedRows();
		int begin = rows[0];
		pos = begin + pos;
		if (pos < 0) {
			pos = 0;
		}
		if (pos > collection.size() - rows.length)
			pos = collection.size() - rows.length;
		List<T> rules = collection;
		List<T> moved = new LinkedList<>();
		for (int i = 0; i < rows.length; i++) {
			moved.add(0, rules.get(rows[i]));
		}
		for (T r: moved) {
			rules.remove(r);
		}
		for (T r: moved) {
			rules.add(pos, r);
		}
		table.setRowSelectionInterval(pos, pos+rows.length - 1);
		table.updateUI();
	}
	
    private void buttonRuleUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRuleUpActionPerformed
		MoveItem(tableAllowRules, config.getSecurity().rules, -1);
    }//GEN-LAST:event_buttonRuleUpActionPerformed

    private void buttonRuleTopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRuleTopActionPerformed
        MoveItem(tableAllowRules, config.getSecurity().rules, -config.getSecurity().rules.size());
    }//GEN-LAST:event_buttonRuleTopActionPerformed

    private void buttonRuleDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRuleDownActionPerformed
        MoveItem(tableAllowRules, config.getSecurity().rules, 1);
    }//GEN-LAST:event_buttonRuleDownActionPerformed

    private void buttonRuleBottomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRuleBottomActionPerformed
        MoveItem(tableAllowRules, config.getSecurity().rules, config.getSecurity().rules.size());
    }//GEN-LAST:event_buttonRuleBottomActionPerformed

    private void buttonMatcherTopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonMatcherTopActionPerformed
        MoveItem(tableMatchers, config.getSecurity().matchers, -config.getSecurity().matchers.size());
    }//GEN-LAST:event_buttonMatcherTopActionPerformed

    private void buttonMatcherUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonMatcherUpActionPerformed
        MoveItem(tableMatchers, config.getSecurity().matchers, -1);
    }//GEN-LAST:event_buttonMatcherUpActionPerformed

    private void buttonMatcherDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonMatcherDownActionPerformed
        MoveItem(tableMatchers, config.getSecurity().matchers, 1);
    }//GEN-LAST:event_buttonMatcherDownActionPerformed

    private void buttonMatcherBottomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonMatcherBottomActionPerformed
        MoveItem(tableMatchers, config.getSecurity().matchers, config.getSecurity().matchers.size());
    }//GEN-LAST:event_buttonMatcherBottomActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddAMQ;
    private javax.swing.JButton buttonAddAllowRule;
    private javax.swing.JButton buttonAddDB;
    private javax.swing.JButton buttonAddMail;
    private javax.swing.JButton buttonAddMatcher;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonDeleteAMQ;
    private javax.swing.JButton buttonDeleteAllowRules;
    private javax.swing.JButton buttonDeleteDB;
    private javax.swing.JButton buttonDeleteMail;
    private javax.swing.JButton buttonDeleteMatcher;
    private javax.swing.JButton buttonMatcherBottom;
    private javax.swing.JButton buttonMatcherDown;
    private javax.swing.JButton buttonMatcherTop;
    private javax.swing.JButton buttonMatcherUp;
    private javax.swing.JButton buttonOK;
    private javax.swing.JButton buttonRuleBottom;
    private javax.swing.JButton buttonRuleDown;
    private javax.swing.JButton buttonRuleTop;
    private javax.swing.JButton buttonRuleUp;
    private javax.swing.JComboBox comboDefaultAction;
    private javax.swing.JComboBox comboSessionType;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel panelAllow;
    private javax.swing.JPanel panelCommon;
    private javax.swing.JTabbedPane panelProperties;
    private javax.swing.JPanel panelURLMatcher;
    private javax.swing.JTable tableAMQ;
    private javax.swing.JTable tableAllowRules;
    private javax.swing.JTable tableDB;
    private javax.swing.JTable tableMail;
    private javax.swing.JTable tableMatchers;
    private javax.swing.JComboBox textAMQConfig;
    private javax.swing.JComboBox textDBConfig;
    private javax.swing.JTextField textSessionTimeout;
    // End of variables declaration//GEN-END:variables
}
