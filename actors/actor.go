/*public interface for actors*/
package actors

type Actor interface {
	HandleWholeMessage(msg []byte, userId int) ([]byte, []byte, []byte)
}
