package response

import (
	"net/http"

	"melonet-backend/internal/domain"

	"github.com/gin-gonic/gin"
)

type Envelope struct {
	Data  any           `json:"data"`
	Error *ErrorBody    `json:"error"`
	Meta  *domain.Pagination `json:"meta,omitempty"`
}

type ErrorBody struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}

func OK(c *gin.Context, data any) {
	c.JSON(http.StatusOK, Envelope{
		Data:  data,
		Error: nil,
	})
}

func OKWithMeta(c *gin.Context, data any, meta domain.Pagination) {
	c.JSON(http.StatusOK, Envelope{
		Data:  data,
		Error: nil,
		Meta:  &meta,
	})
}

func Created(c *gin.Context, data any) {
	c.JSON(http.StatusCreated, Envelope{
		Data:  data,
		Error: nil,
	})
}

func Error(c *gin.Context, status int, code, message string) {
	c.JSON(status, Envelope{
		Data: nil,
		Error: &ErrorBody{
			Code:    code,
			Message: message,
		},
	})
}

func BadRequest(c *gin.Context, code, message string) {
	Error(c, http.StatusBadRequest, code, message)
}

func InternalError(c *gin.Context, message string) {
	Error(c, http.StatusInternalServerError, "internal_error", message)
}

func NotFound(c *gin.Context, message string) {
	Error(c, http.StatusNotFound, "not_found", message)
}
