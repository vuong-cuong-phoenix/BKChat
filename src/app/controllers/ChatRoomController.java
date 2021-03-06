package app.controllers;

import app.models.Message;
import app.models.User;
import app.socket.Client;
import app.socket.PeerHandler;
import app.views.ChatPane;
import app.views.UserListViewCell;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class ChatRoomController implements Initializable {
    @FXML
    private TextField tf_AddFriend;

    @FXML
    private FontAwesomeIconView btn_AddFriend;

    @FXML
    private ListView<User> lv_UserList;

    @FXML
    private Button btn_Logout;

    @FXML
    private StackPane stack_ChatPane;

    @FXML
    private Text txt_LoggedUsername;

    @FXML
    void logout(ActionEvent event) throws IOException {
        client.logout();
        client.loadSigninFXML();
    }

    private Client client;

    private PeerHandler peerHandler;

    public ObservableList<User> userObservableList = FXCollections.observableArrayList();

    private List<ChatPane> chatPaneList = new ArrayList<>();

    private Alert addFriendAlert = new Alert(Alert.AlertType.INFORMATION);

    private Alert acceptFriendAlert = new Alert(Alert.AlertType.CONFIRMATION);

    public ChatRoomController(Client client) {
        this.client = client;
    }

    private void addFriend(String friend) {
        if (client.getLoggedUser().getUserName().equalsIgnoreCase(friend)) {
            addFriendAlert.setHeaderText("Lỗi! Không thể kết bạn với chính mình!");
            addFriendAlert.showAndWait();
        } else {
            client.addFriend(friend);
        }
    }

    public void resetChatPane() {
        lv_UserList.getSelectionModel().clearSelection();
        //stack_ChatPane.getChildren().clear();

        List<ChatPane> chatPaneList = new ArrayList<>();
        for (Map.Entry<PeerHandler, ObservableList<Message>> entry : client.mapPeerMessageList.entrySet()) {
            ChatPane chatPane = new ChatPane(entry.getKey(), entry.getValue());
            chatPaneList.add(chatPane);
        }
        stack_ChatPane.getChildren().setAll(chatPaneList);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        tf_AddFriend.setOnAction(event -> {
            this.addFriend(tf_AddFriend.getText());
        });

        btn_AddFriend.setOnMouseClicked(event -> {
            this.addFriend(tf_AddFriend.getText());
        });

        txt_LoggedUsername.setText(client.getLoggedUser().getUserName());

        client.addFriendResultProperty().addListener((observableValue, oldValue, newValue) -> {
            switch (newValue.substring(0, newValue.indexOf(","))) {
                case "Success":
                    addFriendAlert.setHeaderText("Chúc mừng, " + tf_AddFriend.getText() + " và bạn đã trở thành bạn bè!");
                    addFriendAlert.show();
                    break;
                case "Failed":
                    addFriendAlert.setHeaderText("Có lỗi xảy ra trong quá trình thêm " + tf_AddFriend.getText() + " làm bạn bè!");
                    addFriendAlert.show();
                    break;
                case "Already":
                    addFriendAlert.setHeaderText("Bạn và " + tf_AddFriend.getText() + " đã là bạn bè rồi!");
                    addFriendAlert.show();
                    break;
            }
        });

        client.acceptFriendProperty().addListener((observableValue, oldValue, newValue) -> {
            String userAddFriend = newValue.substring(0, newValue.indexOf(","));
            acceptFriendAlert.setHeaderText(userAddFriend + " muốn trở thành bạn bè với bạn. Bạn có đồng ý không?");

            Optional<ButtonType> optional = acceptFriendAlert.showAndWait();
            if (optional.isPresent() && optional.get() == ButtonType.CANCEL) {
                client.friendResponse(false, userAddFriend, client.getLoggedUser().getUserName());
            } else if (optional.isPresent() && optional.get() == ButtonType.OK) {
                client.friendResponse(true, userAddFriend, client.getLoggedUser().getUserName());
            }
        });

        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        lv_UserList.getItems().clear();

        Platform.runLater(() -> {
            lv_UserList.setItems(client.getUserObservableList());
            lv_UserList.setCellFactory(userListView -> new UserListViewCell());
        });

        lv_UserList.getSelectionModel().selectedItemProperty().addListener((observableValue, oldUser, newUser) -> {
            if (newUser != null) {
                client.connectToPeer(client.getLoggedUser().getUserName(), newUser.getUserName());

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //stack_ChatPane.getChildren().clear();

                List<ChatPane> chatPaneList = new ArrayList<>();
                for (Map.Entry<PeerHandler, ObservableList<Message>> entry : client.mapPeerMessageList.entrySet()) {
                    ChatPane chatPane = new ChatPane(entry.getKey(), entry.getValue());
                    chatPaneList.add(chatPane);
                }
                if (!chatPaneList.isEmpty()) {
                    stack_ChatPane.getChildren().setAll(chatPaneList);

                    for (Node node : stack_ChatPane.getChildren()) {
                        ChatPane chatPane = (ChatPane) node;
                        if (chatPane.getPeerHandler().getPeerUser().getUserName().equals(newUser.getUserName())) {
                            chatPane.setVisible(true);
                        } else if (oldUser != null && chatPane.getPeerHandler().getPeerUser().getUserName().equals(oldUser.getUserName())) {
                            chatPane.setVisible(false);
                        }
                    }
                }
            }
        });
    }


    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public PeerHandler getPeerHandler() {
        return peerHandler;
    }

    public void setPeerHandler(PeerHandler peerHandler) {
        this.peerHandler = peerHandler;
    }

//    public List<User> getUserList() {
//        return userList;
//    }
//
//    public void setUserList(List<User> userList) {
//        this.userList = userList;
//    }

    public ObservableList<User> getUserObservableList() {
        synchronized (this) {
            return userObservableList;
        }

    }

    public void setUserObservableList(ObservableList<User> userObservableList) {
        synchronized (this) {
            this.userObservableList = userObservableList;
        }

    }
}
