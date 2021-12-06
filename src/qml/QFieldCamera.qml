import QtQuick 2.12
import QtQuick.Controls 2.12
import QtMultimedia 5.14

import Theme 1.0

Item{
  id : cameraItem
  signal finished(string path)
  signal canceled()

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
    Video {
        id: cam1Stream
        x: 49
        y: 91
        width: 505
        height: 336
        source: "http://192.168.25.1:8080/?action=stream"
        autoPlay: true
        opacity: 1.0
        fillMode: Image.Stretch
        muted: false
    }
  Camera {
    id: camera

    position: Camera.BackFace

    imageCapture {
      onImageSaved: {
        currentPath  = path
      }
      onImageCaptured: {
        photoPreview.source = cam1Stream
        cameraItem.state = "PhotoPreview"
      }
    }
  }

  VideoOutput {
    anchors.fill: parent

    visible: cameraItem.state == "PhotoCapture"

    focus : visible
    source: camera

    autoOrientation: true

    MouseArea {
      anchors.fill: parent

      onClicked: {
        if (camera.lockStatus == Camera.Unlocked)
          camera.searchAndLock();
        else
          camera.unlock();
      }
    }


    QfToolButton {
      id: videoButtonClick
      visible: true

      anchors.right: parent.right
      anchors.verticalCenter: parent.verticalCenter

      round: true
      roundborder: true
      bgcolor: "grey"
      borderColor: Theme.mainColor

      onClicked: camera.imageCapture.captureToLocation(qgisProject.homePath+ '/DCIM/')
    }
  }

  Image {
    id: photoPreview

    visible: cameraItem.state == "PhotoPreview"

    anchors.fill: parent

    fillMode: Image.PreserveAspectFit
    smooth: true
    focus: visible

    QfToolButton {
      id: buttonok
      visible: true

      anchors.right: parent.right
      anchors.verticalCenter: parent.verticalCenter
      bgcolor: Theme.mainColor
      round: true

      iconSource: Theme.getThemeIcon("ic_save_white_24dp")

      onClicked: cameraItem.finished( currentPath )
    }

    QfToolButton {
      id: buttonnok
      visible: true

      anchors.right: parent.right
      anchors.top: parent.top
      bgcolor: Theme.mainColor
      round: true

      iconSource: Theme.getThemeIcon("ic_clear_white_24dp")
      onClicked: {
        platformUtilities.rmFile( currentPath )
        cameraItem.state = "PhotoCapture"
      }
    }
  }
}
