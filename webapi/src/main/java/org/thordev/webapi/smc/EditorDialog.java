/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi.smc;

import java.io.File;
import static java.lang.System.exit;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;
import org.thordev.webapi.amq.AMQConfig.AMQSetting.AMQSettingItem;
import org.thordev.webapi.database.DBConfig.DBSetting.DBSettingItem;
import org.thordev.webapi.mail.MailConfig.MailSetting.MailSettingItem;
import org.thordev.webapi.security.SecuritySetting;
import org.thordev.webapi.security.SecuritySetting.Rule;
import org.thordev.webapi.security.SecuritySetting.URLMatcher;


class MatcherTableModel extends AbstractTableModel  {

	private final String[] columns;
	private final List<URLMatcher> matchers;
	public MatcherTableModel(List<URLMatcher> matchers) {
		this.matchers = matchers;
		this.columns = new String [] {
			"URL", "Protocol", "Domain", "Port", "Method"
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
				return matcher.url == null || matcher.url.trim().isEmpty() ? "<any url>" : matcher.url;
			case 1:
				return matcher.scheme == null ? "<any scheme>" : matcher.scheme;
			case 2:
				return matcher.domain == null ? "<any domain>" : matcher.domain;
			case 3:
				return matcher.port == null ? "<any port>" : matcher.port;
			case 4:
				return matcher.method == null ? "<any method>" : matcher.method;
		}
		return null;
	}
	
	@Override
	public String getColumnName(int col) {
		return columns[col];
    }
}

class RuleTableModel extends AbstractTableModel  {

	private final String[] columns;
	private final List<Rule> rules;
	public RuleTableModel(List<Rule> rules) {
		this.rules = rules;
		this.columns = new String [] {
			"Resource Type", "Resource ID", "Operation", "Scenario", "Role", "User"
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
				return rule.resType;
			case 1:
				return rule.resId == null ? "<any id>" : rule.resId;
			case 2:
				return rule.operation == null ? "<any operation>" : rule.operation;
			case 3:
				return rule.scenario == null ? "<any scenario>" : rule.scenario;
			case 4:
				return rule.role == null ? "<no role>" : rule.role;
			case 5:
				return rule.user == null ? "<no user>" : rule.user;
		}
		return null;
	}
	
	@Override
	public String getColumnName(int col) {
		return columns[col];
    }
}

abstract class MapTableModel<T> extends AbstractTableModel {
	private final String[] columns;
	private final Map<String, T> items;
	public MapTableModel(Map<String, T> items, String[] columns) {
		this.items = items;
		this.columns = columns;
		
	}
	
	public Map.Entry<String, T> get(int rowIndex) {
		Object[] array = items.entrySet().toArray();
		Map.Entry<String, T> entry = (Map.Entry<String, T>)array[rowIndex];
		return entry;
	}
	
	public int getKeyIndex(String key) {
		Object[] array = items.entrySet().toArray();
		for (int i = 0; i < array.length; i++) {
			Map.Entry<String, T> entry = (Map.Entry<String, T>)array[i];
			if (entry.getKey().equals(key))
				return i;
		}
		return -1;
	}
	
	@Override
	public int getRowCount() {
		return items.size();
	}

	@Override
	public int getColumnCount() {
		return columns.length;
	}
	
	protected abstract String getColValue(int columnIndex, Map.Entry<String, T> entry);

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return getColValue(columnIndex, get(rowIndex));
	}
	
	@Override
	public String getColumnName(int col) {
		return columns[col];
    }
}


class DBModel extends MapTableModel<DBSettingItem> {
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

class AMQModel extends MapTableModel<AMQSettingItem> {
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

class MailModel extends MapTableModel<MailSettingItem> {
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
	private RuleTableModel allowModel;
	private RuleTableModel denyModel;
	private RuleTableModel advanceModel;
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
        jLabel6 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        textDBConfig = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        textAMQConfig = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        comboSessionType = new javax.swing.JComboBox();
        comboDefaultAction = new javax.swing.JComboBox();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        panelAllow = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableAllowRules = new javax.swing.JTable();
        buttonAddAllowRule = new javax.swing.JButton();
        buttonDeleteAllowRules = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        panelDeny = new javax.swing.JPanel();
        buttonAddDenyRule = new javax.swing.JButton();
        buttonDeleteDenyRules = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jScrollPane9 = new javax.swing.JScrollPane();
        tableDenyRules = new javax.swing.JTable();
        panelAdvanced = new javax.swing.JPanel();
        buttonAddAdvancedRule = new javax.swing.JButton();
        buttonDeleteAdvancedRules = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        jScrollPane10 = new javax.swing.JScrollPane();
        tableAdvancedRules = new javax.swing.JTable();
        jLabel14 = new javax.swing.JLabel();
        panelURLMatcher = new javax.swing.JPanel();
        buttonAddMatcher = new javax.swing.JButton();
        buttonDeleteMatcher = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        tableMatchers = new javax.swing.JTable();
        jLabel11 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        labelMatcherInfo = new javax.swing.JLabel();

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
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 709, Short.MAX_VALUE))
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
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 319, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 709, Short.MAX_VALUE))
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
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 319, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 705, Short.MAX_VALUE))
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
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 319, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Mail Service", jPanel5);

        jPanel4.setOpaque(false);

        panelCommon.setBackground(new java.awt.Color(255, 255, 255));

        textSessionTimeout.setName(""); // NOI18N

        jLabel4.setText("Session Timeout");

        jLabel6.setForeground(new java.awt.Color(153, 153, 153));
        jLabel6.setText("<html>Session is invalid after passed specified seconds</html>");
        jLabel6.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        jLabel1.setText("Database Config");

        textDBConfig.setName(""); // NOI18N

        jLabel2.setText("ActiveMQ Config");

        jLabel12.setForeground(new java.awt.Color(153, 153, 153));
        jLabel12.setText("<html>Provide database configuration name to store the security setting in database and let security module can check privilege from database when matched an advanced rule.</html>");
        jLabel12.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        jLabel13.setForeground(new java.awt.Color(153, 153, 153));
        jLabel13.setText("<html>If you want to notify all front-side load balance server to obtain newly changed settings, then you should specify ActiveMQ configuration name, when save this setting this tool will send a notification to all server which listen to the same ActiveMQ.</html>");
        jLabel13.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        jLabel15.setText("Session Type");

        jLabel16.setForeground(new java.awt.Color(153, 153, 153));
        jLabel16.setText("<html>How to save the session state, use client side cookie or server session</html>");
        jLabel16.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        comboSessionType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Client Side Session", "Server Side Session" }));

        comboDefaultAction.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Allow", "Deny" }));
        comboDefaultAction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboDefaultActionActionPerformed(evt);
            }
        });

        jLabel17.setText("<html><span style=\"color:#888888;\">When no rules can match the operation. </span></html>");
        jLabel17.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        jLabel18.setText("Default Action");

        javax.swing.GroupLayout panelCommonLayout = new javax.swing.GroupLayout(panelCommon);
        panelCommon.setLayout(panelCommonLayout);
        panelCommonLayout.setHorizontalGroup(
            panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCommonLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelCommonLayout.createSequentialGroup()
                        .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(textDBConfig, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)))
                        .addGap(65, 65, 65)
                        .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(textAMQConfig, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE)))
                    .addComponent(jSeparator1)
                    .addGroup(panelCommonLayout.createSequentialGroup()
                        .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(textSessionTimeout))
                        .addGap(18, 23, Short.MAX_VALUE)
                        .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(comboSessionType, 0, 203, Short.MAX_VALUE)
                            .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 22, Short.MAX_VALUE)
                        .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(comboDefaultAction, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jLabel17, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 208, Short.MAX_VALUE)
                                .addComponent(jLabel18, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 320, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(393, 393, 393))
        );
        panelCommonLayout.setVerticalGroup(
            panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCommonLayout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel15)
                    .addComponent(jLabel18))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jLabel17)
                    .addComponent(jLabel16))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(textSessionTimeout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(comboSessionType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(comboDefaultAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(31, 31, 31)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel12)
                    .addComponent(jLabel13))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(textAMQConfig, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textDBConfig, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(48, 48, 48))
        );

        panelProperties.addTab("Common", panelCommon);

        panelAllow.setBackground(new java.awt.Color(255, 255, 255));

        tableAllowRules.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Resource Type", "Resource ID", "Scenario", "Role", "User", "Operation"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
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

        jLabel8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/allow.png"))); // NOI18N

        javax.swing.GroupLayout panelAllowLayout = new javax.swing.GroupLayout(panelAllow);
        panelAllow.setLayout(panelAllowLayout);
        panelAllowLayout.setHorizontalGroup(
            panelAllowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAllowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelAllowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAllowLayout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonAddAllowRule)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonDeleteAllowRules)))
                .addContainerGap())
        );
        panelAllowLayout.setVerticalGroup(
            panelAllowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAllowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelAllowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel8)
                    .addGroup(panelAllowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(buttonAddAllowRule)
                        .addComponent(buttonDeleteAllowRules)))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        panelProperties.addTab("Allow", panelAllow);

        panelDeny.setBackground(new java.awt.Color(255, 255, 255));

        buttonAddDenyRule.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/add.png"))); // NOI18N
        buttonAddDenyRule.setText("Add Rule");
        buttonAddDenyRule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddDenyRuleActionPerformed(evt);
            }
        });

        buttonDeleteDenyRules.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/delete.png"))); // NOI18N
        buttonDeleteDenyRules.setText("Delete");
        buttonDeleteDenyRules.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteDenyRulesActionPerformed(evt);
            }
        });

        jLabel9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/deny.png"))); // NOI18N

        tableDenyRules.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Resource Type", "Resource ID", "Role", "User", "Operation"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tableDenyRules.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableDenyRulesMouseClicked(evt);
            }
        });
        jScrollPane9.setViewportView(tableDenyRules);

        javax.swing.GroupLayout panelDenyLayout = new javax.swing.GroupLayout(panelDeny);
        panelDeny.setLayout(panelDenyLayout);
        panelDenyLayout.setHorizontalGroup(
            panelDenyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelDenyLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelDenyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelDenyLayout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonAddDenyRule)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonDeleteDenyRules))
                    .addComponent(jScrollPane9, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelDenyLayout.setVerticalGroup(
            panelDenyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelDenyLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelDenyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel9)
                    .addGroup(panelDenyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(buttonAddDenyRule)
                        .addComponent(buttonDeleteDenyRules)))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane9, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        panelProperties.addTab("Deny", panelDeny);

        panelAdvanced.setBackground(new java.awt.Color(255, 255, 255));

        buttonAddAdvancedRule.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/add.png"))); // NOI18N
        buttonAddAdvancedRule.setText("Add Rule");
        buttonAddAdvancedRule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddAdvancedRuleActionPerformed(evt);
            }
        });

        buttonDeleteAdvancedRules.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/delete.png"))); // NOI18N
        buttonDeleteAdvancedRules.setText("Delete");
        buttonDeleteAdvancedRules.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteAdvancedRulesActionPerformed(evt);
            }
        });

        jLabel10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thordev/webapi/smc/advance.png"))); // NOI18N

        tableAdvancedRules.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Resource Type", "Resource ID", "Role", "User", "Operation"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tableAdvancedRules.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableAdvancedRulesMouseClicked(evt);
            }
        });
        jScrollPane10.setViewportView(tableAdvancedRules);

        jLabel14.setText("Advanced rule will be run in database.");

        javax.swing.GroupLayout panelAdvancedLayout = new javax.swing.GroupLayout(panelAdvanced);
        panelAdvanced.setLayout(panelAdvancedLayout);
        panelAdvancedLayout.setHorizontalGroup(
            panelAdvancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAdvancedLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelAdvancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAdvancedLayout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(jLabel10)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonAddAdvancedRule)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonDeleteAdvancedRules))
                    .addComponent(jScrollPane10, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        panelAdvancedLayout.setVerticalGroup(
            panelAdvancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAdvancedLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelAdvancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelAdvancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel14)
                        .addComponent(buttonAddAdvancedRule)
                        .addComponent(buttonDeleteAdvancedRules)))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane10, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        panelProperties.addTab("Advanced", panelAdvanced);

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

        tableMatchers.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "URL", "Protocal", "Domain", "Port", "Method"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
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

        labelMatcherInfo.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        labelMatcherInfo.setAutoscrolls(true);
        jScrollPane2.setViewportView(labelMatcherInfo);

        javax.swing.GroupLayout panelURLMatcherLayout = new javax.swing.GroupLayout(panelURLMatcher);
        panelURLMatcher.setLayout(panelURLMatcherLayout);
        panelURLMatcherLayout.setHorizontalGroup(
            panelURLMatcherLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelURLMatcherLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelURLMatcherLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelURLMatcherLayout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonAddMatcher)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonDeleteMatcher))
                    .addComponent(jScrollPane2))
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
                .addGap(18, 18, 18)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(42, 42, 42))
        );

        panelProperties.addTab("URL Matcher", panelURLMatcher);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelProperties, javax.swing.GroupLayout.PREFERRED_SIZE, 709, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelProperties, javax.swing.GroupLayout.PREFERRED_SIZE, 364, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Security Setting", jPanel4);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 738, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonOK, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(buttonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 421, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonOK))
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void buttonAddAllowRuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddAllowRuleActionPerformed
        RuleDialog dialog = new RuleDialog(this, true);
		dialog.setVisible(true);
		Rule rule = dialog.getRule();
		if (rule != null) {
			config.getSecurity().allowRules.add(rule);
			allowModel.fireTableDataChanged();
			tableAllowRules.setRowSelectionInterval(config.getSecurity().allowRules.size() - 1, config.getSecurity().allowRules.size() - 1);
		}
    }//GEN-LAST:event_buttonAddAllowRuleActionPerformed

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
		this.allowModel = new RuleTableModel(config.getSecurity().allowRules);
		tableAllowRules.setModel(allowModel);
		this.denyModel = new RuleTableModel(config.getSecurity().denyRules);
		tableDenyRules.setModel(denyModel);
		this.advanceModel = new RuleTableModel(config.getSecurity().dbRules);
		tableAdvancedRules.setModel(advanceModel);
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
		textDBConfig.setText(config.getSecurity().dbConfig);
		textAMQConfig.setText(config.getSecurity().amqConfig);
    }//GEN-LAST:event_formWindowOpened

    private void buttonAddMatcherActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddMatcherActionPerformed
        MatcherDialog dialog = new MatcherDialog(this, true);
		dialog.setVisible(true);
		URLMatcher matcher = dialog.getMatcher();
		if (matcher != null) {
			config.getSecurity().matchers.add(matcher);
			matcherModel.fireTableDataChanged();
			tableMatchers.setRowSelectionInterval(config.getSecurity().matchers.size() - 1, config.getSecurity().matchers.size() - 1);
			labelMatcherInfo.setText(getMathcerDescription(matcher));
		}
    }//GEN-LAST:event_buttonAddMatcherActionPerformed

    private void buttonDeleteAllowRulesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteAllowRulesActionPerformed
        List<Rule> removed = new LinkedList<>();
		for (int rownum : tableAllowRules.getSelectedRows()) {
			removed.add(config.getSecurity().allowRules.get(rownum));
		}
		config.getSecurity().allowRules.removeAll(removed);
		allowModel.fireTableDataChanged();
    }//GEN-LAST:event_buttonDeleteAllowRulesActionPerformed

    private void buttonAddDenyRuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddDenyRuleActionPerformed
        RuleDialog dialog = new RuleDialog(this, true);
		dialog.setVisible(true);
		Rule rule = dialog.getRule();
		if (rule != null) {
			config.getSecurity().denyRules.add(rule);
			denyModel.fireTableDataChanged();
			tableDenyRules.setRowSelectionInterval(config.getSecurity().denyRules.size() - 1, config.getSecurity().denyRules.size() - 1);
		}
    }//GEN-LAST:event_buttonAddDenyRuleActionPerformed

    private void buttonDeleteDenyRulesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteDenyRulesActionPerformed
        List<Rule> removed = new LinkedList<>();
		for (int rownum : tableDenyRules.getSelectedRows()) {
			removed.add(config.getSecurity().denyRules.get(rownum));
		}
		config.getSecurity().denyRules.removeAll(removed);
		denyModel.fireTableDataChanged();
    }//GEN-LAST:event_buttonDeleteDenyRulesActionPerformed

    private void buttonAddAdvancedRuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddAdvancedRuleActionPerformed
        RuleDialog dialog = new RuleDialog(this, true);
		dialog.setVisible(true);
		Rule rule = dialog.getRule();
		if (rule != null) {
			config.getSecurity().dbRules.add(rule);
			advanceModel.fireTableDataChanged();
			tableAdvancedRules.setRowSelectionInterval(config.getSecurity().dbRules.size() - 1, config.getSecurity().dbRules.size() - 1);
		}
    }//GEN-LAST:event_buttonAddAdvancedRuleActionPerformed

    private void buttonDeleteAdvancedRulesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteAdvancedRulesActionPerformed
        List<Rule> removed = new LinkedList<>();
		for (int rownum : tableAdvancedRules.getSelectedRows()) {
			removed.add(config.getSecurity().dbRules.get(rownum));
		}
		config.getSecurity().dbRules.removeAll(removed);
		advanceModel.fireTableDataChanged();
    }//GEN-LAST:event_buttonDeleteAdvancedRulesActionPerformed

    private void buttonDeleteMatcherActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteMatcherActionPerformed
        List<URLMatcher> removed = new LinkedList<>();
		for (int rownum : tableMatchers.getSelectedRows()) {
			removed.add(config.getSecurity().matchers.get(rownum));
		}
		config.getSecurity().matchers.removeAll(removed);
		matcherModel.fireTableDataChanged();
		labelMatcherInfo.setText("");
    }//GEN-LAST:event_buttonDeleteMatcherActionPerformed

    private void tableAllowRulesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableAllowRulesMouseClicked
        if (evt.getClickCount() == 2) {
			int rowId = tableAllowRules.getSelectedRow();
			Rule rule = config.getSecurity().allowRules.get(rowId);
			RuleDialog dialog = new RuleDialog(this, true);
			dialog.setRule(rule);
			dialog.setVisible(true);
			if (dialog.isOk()) {
				allowModel.fireTableDataChanged();
				tableAllowRules.setRowSelectionInterval(rowId, rowId);
			}
		}
    }//GEN-LAST:event_tableAllowRulesMouseClicked

    private void tableDenyRulesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableDenyRulesMouseClicked
        if (evt.getClickCount() == 2) {
			int rowId = tableDenyRules.getSelectedRow();
			Rule rule = config.getSecurity().denyRules.get(rowId);
			RuleDialog dialog = new RuleDialog(this, true);
			dialog.setRule(rule);
			dialog.setVisible(true);
			if (dialog.isOk()) {
				denyModel.fireTableDataChanged();
				tableDenyRules.setRowSelectionInterval(rowId, rowId);
			}
		}
    }//GEN-LAST:event_tableDenyRulesMouseClicked

    private void tableAdvancedRulesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableAdvancedRulesMouseClicked
        if (evt.getClickCount() == 2) {
			int rowId = tableAdvancedRules.getSelectedRow();
			Rule rule = config.getSecurity().dbRules.get(rowId);
			RuleDialog dialog = new RuleDialog(this, true);
			dialog.setRule(rule);
			dialog.setVisible(true);
			if (dialog.isOk()) {
				advanceModel.fireTableDataChanged();
				tableAdvancedRules.setRowSelectionInterval(rowId, rowId);
			}
		}
    }//GEN-LAST:event_tableAdvancedRulesMouseClicked

	private String getMathcerDescription(URLMatcher matcher) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><div style='color:blue;font-weight:bold;font-size:12px;border-bottom:1px solid #888888;padding:2px 0px;margin:2px 0px;'>Mapping To:</div>");
		if (matcher.resType != null && !matcher.resType.trim().isEmpty()) {
			sb.append("<div><span style='font-weight:bold;'>Resource:</span> ");
			sb.append(matcher.resType);
			sb.append("</div>");
		}
		if (matcher.resId != null && !matcher.resId.trim().isEmpty()) {
			sb.append("<div><span style='font-weight:bold;'>Resource ID:</span> ");
			sb.append(matcher.resId);
			sb.append("</div>");
		}
		if (matcher.operation != null && !matcher.operation.trim().isEmpty()) {
			sb.append("<div><span style='font-weight:bold;'>Operation:</span> ");
			sb.append(matcher.operation);
			sb.append("</div>");
		}
		if (matcher.scenario != null && !matcher.scenario.trim().isEmpty()) {
			sb.append("<div><span style='font-weight:bold;'>Scenario:</span> ");
			sb.append(matcher.scenario);
			sb.append("</div>");
		}
		sb.append("</html>");
		return sb.toString();
	}
	
    private void tableMatchersMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMatchersMouseClicked
        int rowId = tableMatchers.getSelectedRow();
		URLMatcher matcher = config.getSecurity().matchers.get(rowId);
		if (evt.getClickCount() == 2) {
			MatcherDialog dialog = new MatcherDialog(this, true);
			dialog.setMatcher(matcher);
			dialog.setVisible(true);
			if (dialog.isOk()) {
				matcherModel.fireTableDataChanged();
				tableMatchers.setRowSelectionInterval(rowId, rowId);
			}
		}
		labelMatcherInfo.setText(getMathcerDescription(matcher));
    }//GEN-LAST:event_tableMatchersMouseClicked

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
		if (textDBConfig.getText().trim().isEmpty())
			securitySetting.dbConfig = null;
		else
			securitySetting.dbConfig = textDBConfig.getText().trim();
		if (textAMQConfig.getText().trim().isEmpty())
			securitySetting.amqConfig = null;
		else
			securitySetting.amqConfig = textAMQConfig.getText().trim();
		
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

    private void comboDefaultActionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboDefaultActionActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_comboDefaultActionActionPerformed

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


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddAMQ;
    private javax.swing.JButton buttonAddAdvancedRule;
    private javax.swing.JButton buttonAddAllowRule;
    private javax.swing.JButton buttonAddDB;
    private javax.swing.JButton buttonAddDenyRule;
    private javax.swing.JButton buttonAddMail;
    private javax.swing.JButton buttonAddMatcher;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonDeleteAMQ;
    private javax.swing.JButton buttonDeleteAdvancedRules;
    private javax.swing.JButton buttonDeleteAllowRules;
    private javax.swing.JButton buttonDeleteDB;
    private javax.swing.JButton buttonDeleteDenyRules;
    private javax.swing.JButton buttonDeleteMail;
    private javax.swing.JButton buttonDeleteMatcher;
    private javax.swing.JButton buttonOK;
    private javax.swing.JComboBox comboDefaultAction;
    private javax.swing.JComboBox comboSessionType;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel labelMatcherInfo;
    private javax.swing.JPanel panelAdvanced;
    private javax.swing.JPanel panelAllow;
    private javax.swing.JPanel panelCommon;
    private javax.swing.JPanel panelDeny;
    private javax.swing.JTabbedPane panelProperties;
    private javax.swing.JPanel panelURLMatcher;
    private javax.swing.JTable tableAMQ;
    private javax.swing.JTable tableAdvancedRules;
    private javax.swing.JTable tableAllowRules;
    private javax.swing.JTable tableDB;
    private javax.swing.JTable tableDenyRules;
    private javax.swing.JTable tableMail;
    private javax.swing.JTable tableMatchers;
    private javax.swing.JTextField textAMQConfig;
    private javax.swing.JTextField textDBConfig;
    private javax.swing.JTextField textSessionTimeout;
    // End of variables declaration//GEN-END:variables
}
