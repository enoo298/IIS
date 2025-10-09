package hr.algebra.client.controller;

import javafx.scene.control.Button;
import javafx.scene.control.TextFormatter;

import hr.algebra.client.service.*;
import hr.algebra.client.xml.XmlItem;
import hr.algebra.client.xml.XmlResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Optional;

public class MainController {


    @FXML private TabPane tabPane;


    @FXML private TextField tfSoapKeyword;
    @FXML private TextField tfSoapLimit;
    @FXML private TextArea taSoapOutput;

    @FXML private Button btnCreate, btnUpdate;

    @FXML private TableView<XmlItem> tvItems;
    @FXML private TableColumn<XmlItem, Integer> colPos;
    @FXML private TableColumn<XmlItem, String> colTitle;
    @FXML private TableColumn<XmlItem, String> colSnippet;
    @FXML private TableColumn<XmlItem, String> colUrl;
    @FXML private TableColumn<XmlItem, String> colDomain;

    @FXML private TextField tfPos, tfTitle, tfSnippet, tfUrl, tfDomain;


    private final ObservableList<XmlItem> items = FXCollections.observableArrayList();


    @FXML private TextArea taXmlInput;
    @FXML private TextArea taValidationOut;


    @FXML private TextField tfCity;
    @FXML private TextArea taRpcOut;


    @FXML private Label lblStatus;

    private AuthService auth;
    private SoapService soapService;
    private SearchResponseApi responseApi;
    private XmlValidationService xmlValidationService;
    private JaxbValidationApi jaxbValidationApi;
    private XmlRpcService xmlRpcService;



    @FXML
    private void initialize() {
        colPos.setCellValueFactory(new PropertyValueFactory<>("position"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colSnippet.setCellValueFactory(new PropertyValueFactory<>("snippet"));
        colUrl.setCellValueFactory(new PropertyValueFactory<>("url"));
        colDomain.setCellValueFactory(new PropertyValueFactory<>("domain"));

        tvItems.setItems(items);
        tvItems.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            fillForm(n);
            refreshFormValidity();
        });


        tfPos.setTextFormatter(new TextFormatter<>(chg ->
                chg.getControlNewText().matches("\\d*") ? chg : null));


        tfTitle.textProperty().addListener((a,b,c) -> refreshFormValidity());   // <-- dodano
        tfSnippet.textProperty().addListener((a,b,c) -> refreshFormValidity()); // <-- dodano
        tfUrl.textProperty().addListener((a,b,c) -> refreshFormValidity());     // <-- dodano
        tfDomain.textProperty().addListener((a,b,c) -> refreshFormValidity());  // <-- dodano

        refreshFormValidity();
    }


    public void setAuthService(AuthService authService) {
        this.auth = authService;
        this.soapService = new SoapService(auth);
        this.responseApi = new SearchResponseApi(auth,"http://localhost:8080");
        this.xmlValidationService = new XmlValidationService("http://localhost:8080", auth);
        this.jaxbValidationApi = new JaxbValidationApi("http://localhost:8080", auth);
        this.xmlRpcService = new XmlRpcService();


        setStatus("Spremno.");
    }



    private void setStatus(String msg) {
        Platform.runLater(() -> lblStatus.setText(msg));
    }

    private void fillForm(XmlItem it) {
        if (it == null) {
            tfPos.clear(); tfTitle.clear(); tfSnippet.clear(); tfUrl.clear(); tfDomain.clear();
            return;
        }
        tfPos.setText(String.valueOf(it.getPosition()));
        tfTitle.setText(Optional.ofNullable(it.getTitle()).orElse(""));
        tfSnippet.setText(Optional.ofNullable(it.getSnippet()).orElse(""));
        tfUrl.setText(Optional.ofNullable(it.getUrl()).orElse(""));
        tfDomain.setText(Optional.ofNullable(it.getDomain()).orElse(""));
    }



    private XmlItem buildItemFromForm(int positionIfMissingMinus1) {
        XmlItem it = new XmlItem();

        int pos = -1;
        try {
            if (!tfPos.getText().isBlank()) pos = Integer.parseInt(tfPos.getText().trim());
        } catch (NumberFormatException ignored) {}
        if (positionIfMissingMinus1 >= 0 && pos < 0) pos = positionIfMissingMinus1;

        if (pos >= 0) it.setPosition(pos);
        it.setTitle(tfTitle.getText());
        it.setSnippet(tfSnippet.getText());
        it.setUrl(tfUrl.getText());
        it.setDomain(tfDomain.getText());
        return it;
    }

    private void reloadItemsAsync() {
        Task<Void> t = new Task<>() {
            @Override protected Void call() throws Exception {
                setStatus("Učitavanje XML-a…");
                XmlResponse resp = responseApi.getAllItems();
                Platform.runLater(() -> {
                    items.setAll(resp.getData());

                    setStatus("Učitano " + items.size() + " itema.");
                });
                return null;
            }
        };
        t.setOnFailed(e -> setStatus("Greška pri učitavanju: " + t.getException().getMessage()));
        new Thread(t).start();
    }


    @FXML
    private void onSoapSearch() {
        final String kw = tfSoapKeyword.getText().trim();


        int tmp;
        try {
            tmp = Integer.parseInt(tfSoapLimit.getText().trim());
        } catch (Exception e) {
            tmp = 10;
        }
        final int lim = tmp;

        taSoapOutput.clear();
        setStatus("SOAP poziv...");

        Task<String> task = new Task<>() {
            @Override protected String call() {
                return soapService.searchSoap(kw, lim);
            }
        };

        task.setOnSucceeded(e -> {
            taSoapOutput.setText(task.getValue());
            setStatus("SOAP OK");
        });
        task.setOnFailed(e -> {
            taSoapOutput.setText("Greška: " + task.getException().getMessage());
            setStatus("SOAP greška");
        });

        new Thread(task, "soap-call").start();
    }




    @FXML private void onLoadItems() { reloadItemsAsync(); }

    @FXML private void onAddItem() {
        if (!isFormValid()) { setStatus("Popuni Title, Snippet, URL i Domain."); return; }
        XmlItem it = buildItemFromForm(-1);
        Task<Void> t = new Task<>() {
            @Override protected Void call() throws Exception {
                setStatus("Kreiranje…");
                responseApi.createItem(it);
                return null;
            }
        };
        t.setOnSucceeded(e -> { setStatus("Kreirano."); reloadItemsAsync(); });
        t.setOnFailed(e -> setStatus("Greška pri kreiranju: " + t.getException().getMessage()));
        new Thread(t).start();
    }

    @FXML private void onUpdateItem() {
        XmlItem selected = tvItems.getSelectionModel().getSelectedItem();

        if (selected == null) { setStatus("Odaberi item u tablici."); return; }

        if (!isFormValid()) { setStatus("Popuni Title, Snippet, URL i Domain."); return; }
        XmlItem it = buildItemFromForm(selected.getPosition());
        Task<Void> t = new Task<>() {
            @Override protected Void call() throws Exception {
                setStatus("Ažuriranje…");
                responseApi.updateItem(selected.getPosition(), it);
                return null;
            }
        };
        t.setOnSucceeded(e -> { setStatus("Ažurirano."); reloadItemsAsync(); });
        t.setOnFailed(e -> setStatus("Greška pri ažuriranju: " + t.getException().getMessage()));
        new Thread(t).start();
    }

    @FXML private void onDeleteItem() {
        XmlItem selected = tvItems.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("Odaberi item u tablici."); return; }

        int pos = selected.getPosition();
        Task<Void> t = new Task<>() {
            @Override protected Void call() throws Exception {
                setStatus("Brisanje…");
                responseApi.deleteItem(pos);
                return null;
            }
        };
        t.setOnSucceeded(e -> { setStatus("Obrisano."); reloadItemsAsync(); });
        t.setOnFailed(e -> setStatus("Greška pri brisanju: " + t.getException().getMessage()));
        new Thread(t).start();
    }




    @FXML private void onValidateXsd() {
        String xml = taXmlInput.getText();
        Task<XmlValidationService.Result> t = new Task<>() {
            @Override protected XmlValidationService.Result call() {
                return xmlValidationService.validateWithXsd(xml);
            }
        };
        t.setOnSucceeded(e -> taValidationOut.setText(
                (t.getValue().ok() ? "OK " : "FAIL ") + t.getValue().status() + "\n" + t.getValue().message()));
        t.setOnFailed(e -> taValidationOut.setText("Greška: " + t.getException().getMessage()));
        new Thread(t).start();
    }

    @FXML private void onValidateRng() {
        String xml = taXmlInput.getText();
        Task<XmlValidationService.Result> t = new Task<>() {
            @Override protected XmlValidationService.Result call() {
                return xmlValidationService.validateWithRng(xml);
            }
        };
        t.setOnSucceeded(e -> taValidationOut.setText(
                (t.getValue().ok() ? "OK " : "FAIL ") + t.getValue().status() + "\n" + t.getValue().message()));
        t.setOnFailed(e -> taValidationOut.setText("Greška: " + t.getException().getMessage()));
        new Thread(t).start();
    }

    @FXML private void onValidateJaxb() {
        Task<JaxbValidationApi.Result> t = new Task<>() {
            @Override protected JaxbValidationApi.Result call() {
                return jaxbValidationApi.validate();
            }
        };
        t.setOnSucceeded(e -> {
            JaxbValidationApi.Result r = t.getValue();
            StringBuilder sb = new StringBuilder();
            sb.append(r.valid() ? "OK " : "FAIL ").append(r.status()).append('\n');
            for (String m : r.messages()) sb.append("- ").append(m).append('\n');
            taValidationOut.setText(sb.toString());
        });
        t.setOnFailed(e -> taValidationOut.setText("Greška: " + t.getException().getMessage()));
        new Thread(t).start();
    }


    @FXML private void onGetTemps() {
        String city = tfCity.getText();
        Task<String> t = new Task<>() {
            @Override protected String call() {
                return xmlRpcService.getTemperatures(city);
            }
        };
        t.setOnSucceeded(e -> taRpcOut.setText(t.getValue()));
        t.setOnFailed(e -> taRpcOut.setText("Greška: " + t.getException().getMessage()));
        new Thread(t).start();
    }



    private boolean isBlank(TextField tf) {
        return tf.getText() == null || tf.getText().trim().isEmpty();
    }


    private boolean isFormValid() {
        return !isBlank(tfTitle)
                && !isBlank(tfSnippet)
                && !isBlank(tfUrl)
                && !isBlank(tfDomain);
    }

    private void refreshFormValidity() {
        boolean valid = isFormValid();
        if (btnCreate != null) btnCreate.setDisable(!valid);
        if (btnUpdate != null) {
            boolean hasSelection = tvItems.getSelectionModel().getSelectedItem() != null;
            btnUpdate.setDisable(!valid || !hasSelection);
        }
    }




}
