

import org.snmp4j.PDU;
import org.snmp4j.smi.OID;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.Timer;

public class SNMPManager
{
    private JTree snmpTree;
    private JPanel panel1;
    JScrollPane scrollPane;
    private JButton loadSNMPInfoButton;
    private JTextField enterAnOIDTextField;
    private JComboBox comboBox1;
    private JTextPane textPane1;
    private JButton openMIBFileButton;
    private JCheckBox periodicallyPlotValueCheckBox;
    private JTextField timespanMustBeIntegerTextField;
    private SNMPTrapReceiver trapReceiver = new SNMPTrapReceiver();
    private Boolean isPlot = false;

    public void populateTree(Map<String, String> infoMap)
    {
        DefaultMutableTreeNode topNode = new DefaultMutableTreeNode("1 - iso");
        snmpTree = new JTree();
        DefaultTreeModel model = new DefaultTreeModel(topNode);
        int iter = 0;
        for (Map.Entry<String, String> entry : infoMap.entrySet())
        {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(entry.getKey());
            model.insertNodeInto(newNode, topNode, iter);
            iter++;
            System.out.println(entry.getKey());
        }
        snmpTree.setModel(model);
        model = (DefaultTreeModel) snmpTree.getModel();
        model.reload();
    }

    public void updateTree(Map<String, String> fInfoMap)
    {
        panel1.updateUI();
        panel1.repaint();
        scrollPane.add(snmpTree, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        scrollPane.revalidate();
        scrollPane.updateUI();
        scrollPane.repaint();
        scrollPane.setViewportView(snmpTree);
        snmpTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        snmpTree.addTreeSelectionListener(new TreeSelectionListener()
        {
            @Override
            public void valueChanged(TreeSelectionEvent e)
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) snmpTree.getLastSelectedPathComponent();
                if (node == null)
                    return;
                Object info = node.getUserObject();
                if (node.isLeaf())
                {
                    OID oid = new OID(info.toString());
                    String oidFormat = oid.format();
                    textPane1.setText(fInfoMap.get(info.toString()));
                }
            }
        });
    }

    public void initUI()
    {
        comboBox1.addItem("Walk");
        comboBox1.addItem("Get");
        comboBox1.addItem("Set");
        comboBox1.addItem("Trap");
        SNMPClient client = new SNMPClient("udp:127.0.0.1/161");
        periodicallyPlotValueCheckBox.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                isPlot = (e.getStateChange() == 1);
                System.out.println(isPlot);
            }
        });
        loadSNMPInfoButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                OID oid;
                if (enterAnOIDTextField.getText().isEmpty() || enterAnOIDTextField.getText().equals("Enter an OID") || !enterAnOIDTextField.getText().startsWith("."))
                {
                    System.out.println("Improper OID string");
                    oid = new OID(".1");
                } else
                    oid = new OID(enterAnOIDTextField.getText());
                if (comboBox1.getSelectedIndex() == 0)
                {
                    Map<String, String> infoMap = new TreeMap<>();
                    infoMap = client.walkTable(oid, client.getTarget());
                    populateTree(infoMap);
                    final Map<String, String> fInfoMap = infoMap;
                    updateTree(fInfoMap);
                } else if (comboBox1.getSelectedIndex() == 1)
                {
                    try
                    {
                        String result = client.getAsString(oid);
                        textPane1.setText(result);
                        if(isPlot)
                        {

                        }
                    } catch (IOException ex)
                    {
                        System.out.println("IO Exception.");
                    }
                } else if (comboBox1.getSelectedIndex() == 2)
                {
                    try
                    {
                        String oidVal = client.getAsString(oid);
                        String newOidVal = (String) JOptionPane.showInputDialog(null, "OID Value: \n" + oidVal, "Set a new value for this OID", JOptionPane.PLAIN_MESSAGE, null, null, null);
                        if (newOidVal != null && newOidVal.length() > 0)
                            client.setOid(oid, newOidVal);// oid.setValue(newOidVal);
                    } catch (IOException ex)
                    {
                        System.out.println(ex.getMessage());
                    }
                } else if (comboBox1.getSelectedIndex() == 3)
                {
                    String trapVal = (String) JOptionPane.showInputDialog(null, "Trap value to send: \n", "Trap Sender", JOptionPane.PLAIN_MESSAGE, null, null, null);
                    trapReceiver.startListen();
                    client.sendTrap(oid, PDU.TRAP, trapVal);
                    Timer t = new Timer();
                    // Delay to synchronize trap results
                    t.schedule(new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            textPane1.setText(trapReceiver.getTrapResponse());
                        }
                    }, 500);
                }
            }
        });
        openMIBFileButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                JFileChooser mibFile = new JFileChooser();
                int resp = mibFile.showOpenDialog(null);
                if (resp == JFileChooser.APPROVE_OPTION)
                {
                    Map<String, String> result = client.loadMibOIDValuesByName(mibFile.getSelectedFile());
                    populateTree(result);
                    final Map<String, String> mibInfoMap = result;
                    updateTree(mibInfoMap);
                }
            }
        });
    }

    SNMPManager(JFrame frame)
    {
        initUI();
        frame.repaint();
        frame.revalidate();
    }

    public static void main(String[] args)
    {
        JFrame frame = new JFrame("SNMPManager");
        frame.setContentPane(new SNMPManager(frame).panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        Dimension windowSize = new Dimension();
        windowSize.width = 600;
        windowSize.height = 600;
        frame.setSize(windowSize);
        frame.setVisible(true);
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                new SNMPManager(frame);
                frame.repaint();
                frame.revalidate();
            }
        });
        frame.repaint();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$()
    {
        panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 5, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane = new JScrollPane();
        panel1.add(scrollPane, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(705, 360), null, 0, false));
        snmpTree = new JTree();
        scrollPane.setViewportView(snmpTree);
        loadSNMPInfoButton = new JButton();
        loadSNMPInfoButton.setText("Gather SNMP OIDs");
        panel1.add(loadSNMPInfoButton, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(705, 32), null, 0, false));
        enterAnOIDTextField = new JTextField();
        enterAnOIDTextField.setText("Enter an OID");
        panel1.add(enterAnOIDTextField, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 5, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(705, 24), null, 0, false));
        comboBox1 = new JComboBox();
        panel1.add(comboBox1, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 5, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textPane1 = new JTextPane();
        panel1.add(textPane1, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        openMIBFileButton = new JButton();
        openMIBFileButton.setText("Open MIB File");
        panel1.add(openMIBFileButton, new com.intellij.uiDesigner.core.GridConstraints(3, 3, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        periodicallyPlotValueCheckBox = new JCheckBox();
        periodicallyPlotValueCheckBox.setText("Periodically plot value");
        panel1.add(periodicallyPlotValueCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        timespanMustBeIntegerTextField = new JTextField();
        timespanMustBeIntegerTextField.setText("Timespan (must be integer)");
        panel1.add(timespanMustBeIntegerTextField, new com.intellij.uiDesigner.core.GridConstraints(1, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return panel1;
    }
}
