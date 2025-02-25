/***************************************************************************
                            feedback.h

                              -------------------
              begin                : January 2021
              copyright            : (C) 2021 by Matthias Kuhn
              email                : matthias@opengis.ch
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

#ifndef FEEDBACK_H
#define FEEDBACK_H

#include <qgsfeedback.h>

/**
 * Wrapper around QgsFeedback to support progress feedback through properties
 */
class Feedback : public QgsFeedback
{
    Q_OBJECT

    /**
     * Progress value. Should be 0 - 100
     */
    Q_PROPERTY( double progress READ progress WRITE setProgress NOTIFY progressChanged )

    /**
     * Current status.
     */
    Q_PROPERTY( QString status READ status WRITE setStatus NOTIFY statusChanged )

    /**
     * Success value to indicate whether an operation was successful or not.
     */
    Q_PROPERTY( bool success READ success WRITE setSuccess NOTIFY successChanged )
  public:
    Feedback();

    QString status() const;
    void setStatus( const QString &status );

    bool success() const;
    void setSuccess( const bool success );

    double progress();

  signals:
    void successChanged();
    void statusChanged();
    void progressChanged();

  private:
    bool mSuccess = false;
    QString mStatus;
    double mProgressProxy = -1;
};

#endif // FEEDBACK_H
