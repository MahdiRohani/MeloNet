package handlers

import (
	"fmt"
	"log"
	"melonet-backend/database"
	"melonet-backend/models"
	"net/http"
	"strconv"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

var clients = make(map[uint]*websocket.Conn)
var clientsMu sync.Mutex

type WSMessage struct {
	SenderID   uint   `json:"sender_id"`
	ReceiverID uint   `json:"receiver_id"`
	Content    string `json:"content"`
	MsgType    string `json:"msg_type"`
}

func HandleChatConnections(c *gin.Context) {

	userIDStr := c.Query("user_id")
	userID, err := strconv.ParseUint(userIDStr, 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "ID کاربر نامعتبر است"})
		return
	}

	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		log.Printf("خطا در ارتقای وب‌سوکت: %v", err)
		return
	}

	clientsMu.Lock()
	clients[uint(userID)] = conn
	clientsMu.Unlock()

	fmt.Printf("⚡ کاربر شماره %d به چت ملو‌نت متصل شد.\n", userID)

	defer func() {
		clientsMu.Lock()
		delete(clients, uint(userID))
		clientsMu.Unlock()
		conn.Close()
		fmt.Printf("❌ کاربر شماره %d دیسکانکت شد.\n", userID)
	}()

	for {
		var wsMsg WSMessage

		err := conn.ReadJSON(&wsMsg)
		if err != nil {
			log.Printf("خطا در خواندن پیام: %v", err)
			break
		}

		dbMessage := models.Message{
			SenderID:   wsMsg.SenderID,
			ReceiverID: wsMsg.ReceiverID,
			Content:    wsMsg.Content,
			MsgType:    wsMsg.MsgType,
			CreatedAt:  time.Now(),
		}
		database.DB.Create(&dbMessage)

		clientsMu.Lock()
		receiverConn, isOnline := clients[wsMsg.ReceiverID]
		clientsMu.Unlock()

		if isOnline {
			err := receiverConn.WriteJSON(dbMessage)
			if err != nil {
				log.Printf("خطا در فوروارد پیام به کاربر %d: %v", wsMsg.ReceiverID, err)
				receiverConn.Close()
			}
		}
	}
}

func GetChatHistory(c *gin.Context) {
	user1 := c.Query("user_id")
	user2 := c.Query("with_id")

	var messages []models.Message

	database.DB.Where(
		"(sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)",
		user1, user2, user2, user1,
	).Order("created_at asc").Find(&messages)

	c.JSON(http.StatusOK, messages)
}
