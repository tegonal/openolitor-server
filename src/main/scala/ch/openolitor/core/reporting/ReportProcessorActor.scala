/*                                                                           *\
*    ____                   ____  ___ __                                      *
*   / __ \____  ___  ____  / __ \/ (_) /_____  _____                          *
*  / / / / __ \/ _ \/ __ \/ / / / / / __/ __ \/ ___/   OpenOlitor             *
* / /_/ / /_/ /  __/ / / / /_/ / / / /_/ /_/ / /       contributed by tegonal *
* \____/ .___/\___/_/ /_/\____/_/_/\__/\____/_/        http://openolitor.ch   *
*     /_/                                                                     *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by           *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package ch.openolitor.core.reporting

import akka.actor._
import scala.util._
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.filestore.FileStore

object ReportProcessorActor {
  def props(fileStore: FileStore, sysConfig: SystemConfig): Props = Props(classOf[ReportProcessorActor], fileStore, sysConfig)
}

/**
 * This actor generates a report document per json data object in a sequence with the same report template
 * object. The same report template object should be shared across all reports. As a result the actor returns a list of successful
 * and unsuccessful sources which might then get processed further
 */
class ReportProcessorActor(fileStore: FileStore, sysConfig: SystemConfig) extends Actor with ActorLogging {
  import ReportProcessorActor._
  import ReportSystem._

  var stats = GenerateReportsStats(None, 0, 0, 0)
  var origSender: Option[ActorRef] = None

  val receive: Receive = {
    case GenerateReports(file, data, false, None) =>
      processReports(file, data, row => SingleDocumentReportProcessorActor.props(row.name, row.locale))
    case GenerateReports(file, data, true, None) =>
      processReports(file, data, row => SingleDocumentReportPDFProcessorActor.props(sysConfig, row.name, row.locale))
    case GenerateReports(file, data, true, Some(option)) =>
      processReports(file, data, row => SingleDocumentStoreReportPDFProcessorActor.props(fileStore, sysConfig, option.fileType, row.id, row.name, row.locale))
  }

  val collectingResults: Receive = {
    case result: ReportResult =>
      receivedResult(result)
  }

  def receivedResult(result: ReportResult) = {
    result match {
      case result: ReportSuccess =>
        stats = stats.copy(numberOfSuccess = stats.numberOfSuccess + 1)
        origSender map (_ ! SingleReportResult(stats, Right(result)))
      //send result direct to client
      case error: ReportError =>
        stats = stats.copy(numberOfFailures = stats.numberOfFailures + 1)
        origSender map (_ ! SingleReportResult(stats, Left(error)))
    }

    stats = stats.copy(numberOfReportsInProgress = stats.numberOfReportsInProgress - 1)

    if (stats.numberOfReportsInProgress <= 0) {
      //send completed result
      origSender map (_ ! stats)
      self ! PoisonPill
    }
  }

  def processReports(file: Array[Byte], data: ReportData[_], f: ReportDataRow => Props) = {
    origSender = Some(sender)
    stats = stats.copy(jobId = Some(data.jobId), numberOfReportsInProgress = data.rows.length)

    for {
      (row, index) <- data.rows.zipWithIndex
    } yield {
      context.actorOf(f(row), s"report-$index-${System.currentTimeMillis}") ! GenerateReport(file, row.value)
    }

    context become collectingResults
  }
}