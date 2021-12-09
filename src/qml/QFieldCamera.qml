import QtQuick 2.12
import QtQuick.Controls 2.12
import QtMultimedia 5.14
import QtQuick.Layouts 1.12
import QtWebView 1.14
import org.qfield 1.0
import Theme 1.0

Item{
  id : cameraItem
  signal finished(string path)
  signal canceled()
  
  property var browserView: undefined
  property string url: 'https://upload.wikimedia.org/wikipedia/commons/7/7d/Australia.jpg'
  property string currentPath

  anchors.fill: parent

  state: "PhotoCapture"

  states: [
    State {
      name: "PhotoCapture"
      StateChangeScript {
        script: {
          camera.captureMode = Camera.CaptureStillImage
        }
      }
    },
    State {
      name: "PhotoPreview"
    }
  ]
Rectangle {
    id: rect1
    width: 300
    height: 400

      Item {
        id: browserContainer
        width: 300
        height: 300
    }
        Timer {
            interval: 500
            running: true
            repeat: true
            onTriggered: {         
        if (visible && url != '') {
            if (browserView === undefined) {
                browserView = Qt.createQmlObject('import QtWebView 1.14; WebView { id: browserView; onLoadingChanged: if ( !loading ) { anchors.fill = parent; } }', browserContainer);
            }
            browserView.url = url;
        }
            }
        }
    }
      QfToolButton {
      id: mainbutton
      visible: true

      anchors.right: parent.right
      anchors.verticalCenter: parent.verticalCenter

      round: true
      roundborder: true
      bgcolor: "grey"
      borderColor: Theme.mainColor

      onClicked: {
             rect1.grabToImage(function(result) {
                           result.saveToFile(qgisProject.homePath+ '/DCIM/3.jpg');
                       })
      cameraItem.finished( currentPath )
      platformUtilities.rmFile( currentPath )
      cameraItem.state = "PhotoCapture"
      }
    }
}