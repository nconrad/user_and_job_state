define(['jquery', 'kbwidget', 'bootstrap', 'userandjobstate', 'jquery.dataTables'], function($) {
    /**
     * @module kbaseUserJobState
     *
     * A reasonably simple widget to demonstrate the user_and_job_state service.
     * 
     * This widget follows the general format of the KBase Functional Site's user page (or 'My Stuff' page), implementing
     * a "Job Status" table view.
     *
     * @exports kbaseUserJobState
     * @version 0.1
     * @author Bill Riehl <wjriehl@lbl.gov>
     */
    $.KBWidget({
        name: "kbaseUserJobState",
        options: {
            auth: null,
            loadingImage: "assets/external/kbase/images/ajax-loader-blue.gif",
            userJobStateURL: "http://140.221.84.180:7083",
        },

        /**
         * @method init
         * Initializes this widget - called by the constructor.
         * @param {object} [options] - this optional object contains any options that need to be passed to the constructor. These include 'auth' an auth token and 'userJobStateURL' the URL of the user_and_job_state service.
         * @private
         */
        init: function(options) {
            this._super(options);

            this.$notLoggedInDiv = $("<div>You are not logged in. Please log in to view your jobs.</div>")
                                   .css({ 'display' : 'none' });
            this.$noJobsDiv = $("<div>You have no running jobs</div>")
                              .css({ 'display' : 'none' });

            this.render();
            this.refresh();
            return this;
        },

        /**
         * @method render
         * This method renders the widget. It is called automatically during the initialization process.
         * The 'render' step here relates to the HTML rendering of the skeleton of what the widget should display.
         * The process of fetching job information and populating the widget is performed by the refresh() method.
         */
        render: function() {
            var self = this;

            // Define and track a loading image spinner.
            this.$loadingImage = $("<img>")
                                 .attr("src", this.options.loadingImage)
                                 .css({"display" : "none"});

            // Define and keep track of the refresh button.
            this.$refreshButton = $("<a>")
                                  .addClass("btn btn-primary btn-sm glyphicon glyphicon-refresh")
                                  .click(function(event) {
                                      self.refresh();
                                  });

            // Define and track the table container (uses Bootstrap 3 and kbaseUserJobState.css)
            this.$tableContainer = $("<div>")
                                   .addClass("row in kbujs-table-container");

            // Build the Modal dropdown (uses Bootstrap 3) that will contain detailed job information.
            // The body of the modal can be accessed with:
            // this.$modal.find('.modal-dialog .modal-content .modal-body')
            this.$modal = $("<div>")
                          .addClass("modal fade")
                          .append($("<div>")
                                  .addClass("modal-dialog")
                                  .append($("<div>")
                                          .addClass("modal-content")
                                          .append($("<div>")
                                                  .addClass("modal-header")
                                                  .append("<button type='button' class='close' data-dismiss='modal' aria-hidden='true'>&times;</button>")
                                                  .append("<h4 class='modal-title'>Job Details</h4>"))
                                          .append($("<div>")
                                                  .addClass("modal-body"))
                                          .append($("<div>")
                                                  .addClass("modal-footer")
                                                  .append("<button type='button' class='btn btn-default btn-primary' data-dismiss='modal'>Close</button>"))));

            // Build the container that will contain the main table and modal.
            // Uses both Bootstrap 3 and kbase_stylesheet.css classes
            var $container = $("<div>")
                             .addClass("table_section container")
                             .append($("<div>")
                                     .addClass("section_title row")
                                     .append($("<div>")
                                             .addClass("title")
                                             .append($("<a>")
                                                     .attr("href", "#")
                                                     .attr("data-toggle", "collapse")
                                                     .attr("data-target", "#job_status_container")
                                                     .append($("<span>")
                                                             .addClass("glyphicon glyphicon-chevron-down"))
                                                     .append("&nbsp;Job Status&nbsp;&nbsp;")
                                                     .append(this.$loadingImage)))
                                     .append($("<div>")
                                             .addClass("col-md-2")
                                             .append(this.$refreshButton)))
                             .append(this.$notLoggedInDiv)
                             .append(this.$noJobsDiv)
                             .append(this.$tableContainer);

            this.$elem.append($container);
            this.$elem.append(this.$modal);
            return this;
        },

        /**
         * @method setAuth
         * Sets a new auth token in the event that a users logs in or out.
         * This also triggers a refresh event.
         *
         * @param {object} token - the authentication token, expected to be a token object
         * as retrieved by the login widget, not just the token string.
         */
        setAuth: function(token) {
            if (token)
                this.options.auth = token;
            else {
                this.options.auth = null;
                this.userJobStateClient = null;
            }

            this.refresh();
        },

        /**
         * @method refresh
         * Refreshes the job status widget. If a user is logged in, then it fetches all of that user's jobs
         * from the user_and_job_state service, and populates the job status table with them.
         *
         * Otherwise, it prompts the user to log in.
         */
        refresh: function() {
            this.$tableContainer.empty();
            this.$notLoggedInDiv.css({ "display" : "none" });
            this.$noJobsDiv.css({ "display" : "none" });
            if (!this.options.auth || this.options.auth === null) {
                this.$notLoggedInDiv.css({ "display" : "" });
                return;
            }

            else {
                this.$loadingImage.css({ "display" : "" });
                this.userJobStateClient = new UserAndJobState(this.options.userJobStateURL, this.options.auth);
                var self = this;

                this.userJobStateClient.list_job_services(
                    function(services) {

                        var getServiceJobs = [];
                        var allJobsList = [];

                        $.each(services, function(index, name) {
                            getServiceJobs.push(self.userJobStateClient.list_jobs([name], '', 
                                function(jobs) {
                                    allJobsList = allJobsList.concat(jobs);
                                },

                                self.clientError
                            ));
                        });

                        $.when.apply($, getServiceJobs).done(function() {
                            var jobTableRows = [];

                            for (var i=0; i<allJobsList.length; i++) {
                                var job = allJobsList[i];
                                jobTableRows.push([
                                    "<button class='btn btn-primary btn-med' job-id='" + job[0] + "'><span class='glyphicon glyphicon-search'/></button>",
                                    job[1],                         // service name
                                    job[12],                        // description
                                    self.makePrettyTimestamp(job[3]),        // started
                                    self.makeStatusElement(job),
                                ]);
                            }

                            if (jobTableRows.length > 0) {
                                self.$jobTable = $("<table id='kbujs-jobs-table'>")
                                                 .addClass("table table-striped table-bordered");

                                self.$jobTable =self.$jobTable.dataTable({
                                    "bLengthChange" : false,
                                    "iDisplayLength": 20,
                                    "aaData" : jobTableRows,
                                    "aoColumns" : [
                                        { "sTitle" : "&nbsp;", "bSortable" : false },
                                        { "sTitle" : "Service" },
                                        { "sTitle" : "Description" },
                                        { "sTitle" : "Started" },
                                        { "sTitle" : "Status" },
                                    ],
                                    "sPaginationType" : "full_numbers",
                                    "aaSorting" : [[1, "asc"]],
                                    "aoColumnDefs" : [
                                    ]
                                });

                                self.$jobTable.on("click", "button[job-id]",
                                    function(event) { 
                                        self.showJobDetails($(event.currentTarget).attr('job-id'));
                                    }
                                );

                                self.$tableContainer.append(self.$jobTable);
                                $("[data-toggle='tooltip']").tooltip({'placement' : 'top'});
                            }
                            else {
                                self.$noJobsDiv.css({ "display" : "" });
                            }
                            self.$loadingImage.css({ "display" : "none" });

                        });
                    },
                    this.clientError
                );
            }
        },

        /**
         * @method makePrettyTimestamp
         * Makes a div containing the 'started time' in units of time ago, with a Bootstrap 3 tooltip
         * that gives the exact time.
         *
         * Note that this tooltip needs to be activated with the $().tooltip() method before it'll function.
         *
         * @param timestamp the timestamp to calculate this div around. Should be in a Date.parse() parseable format.
         * @param suffix an optional suffix for the time element. e.g. "ago" or "from now".
         * @return a div element with the timestamp calculated to be in terms of how long ago, with a tooltip containing the exact time.
         * @private
         */
        makePrettyTimestamp: function(timestamp, suffix) {
            var parsedTime = this.parseTimeStamp(timestamp);
            var timediff = this.calcTimeDifference(timestamp);
            if (suffix)
                timediff += " " + suffix;

            var timeHtml = "<div href='#' data-toggle='tooltip' title='" + parsedTime + "'>" + timediff + "</div>";
            return timeHtml;
        },

        /**
         * @method makeStatusElement
         * Builds the HTML for a Status element based on the given job object.
         * Cases:
         * 1. Job complete - return 'complete + status message'
         * 2. Error - return 'error' as a clickable link - opens a modal with the error message.
         * 3. not complete OR error = in progress.
         *    Show 3 rows. First = status + progress text ('x / y' or 'z%'). Second = progress bar. Bottom = time remaining div.
         *
         * This is all returned wrapped in a div element.
         * @param job - the job to build a status element around.
         * @return a div element containing the job's status.
         */
        makeStatusElement: function(job) {
            var status = "<div>";
            if (job[11] === 1)
                status += "Error";
            else if (job[10] === 1)
                status += "Complete: " + job[4];
            else {
                status += "<div>" + job[4];
                var progressType = job[8].toLowerCase();
                var progress = job[6];
                var max = job[7];

                if (progressType === "percent") {
                    status += " (" + progress + "%)</div>";
                }
                if (progressType === "task") {
                    status += " (" + progress + " / " + max + ")</div>";
                }
                if (progressType !== "none") {
                    status += "</div>" + this.makeProgressBarElement(job);
                }

                if (job[9] != null) {
                    status += this.makePrettyTimestamp(job[9], " remaining");
                }
            }
            return status + "</div>";
        },

        makeProgressBarElement: function(job) {
            var type = job[8].toLowerCase();
            var max = job[7];
            var progress = job[6];

            if (type === "percent") {
                return "<div class='progress' style='margin-bottom: 0;'>" + 
                           "<div class='progress-bar' role='progressbar' aria-valuenow='" + 
                               progress + "' aria-valuemin='0' aria-valuemax='100' style='width: " + 
                               progress + "%;'>" +
                               "<span class='sr-only'>" + progress + "% Complete" + "</span>" +
                           "</div>" +
                       "</div>";
            }
            else {
                return "<div class='progress' style='margin-bottom: 0;'>" + 
                           "<div class='progress-bar' role='progressbar' aria-valuenow='" + 
                           progress + "' aria-valuemin='0' aria-valuemax='" + max + "' style='width: " + 
                           (progress / max * 100) + "%;'>" +
                               "<span class='sr-only'>" + progress + " / " + max + "</span>" +
                           "</div>" +
                       "</div>";
            }
            return "<div></div>";
        },


        /**
         * Shows the details of the job with the given ID in the modal dialog.
         *
         * @param jobId the ID of the given job.
         */
        showJobDetails: function(jobId) {
            var tableRow = function(elems) {
                var row = "<tr>";
                for (var i=0; i<elems.length; i++) {
                    row += "<td>" + elems[i] + "</td>";
                }
                row += "</tr>";
                return row;
            };

            var self = this;

            self.userJobStateClient.get_job_info(jobId, 
                function(job) {

                    // Makes and returns a Bootstrap progress bar based on job information.

                    // Parses the results section of the job into something linkable.
                    var parseResults = function(results) {
                        if (results && results !== null) {

                        }

                        return null;
                    };

                    var $infoTable = $("<table>")
                                     .addClass("table table-striped table-bordered")
                                     .append(tableRow(["Job ID", job[0]]))
                                     .append(tableRow(["Service", job[1]]))
                                     .append(tableRow(["Description", job[12]]))
                                     .append(tableRow(["Stage", job[2]]))
                                     .append(tableRow(["Status", job[4]]))
                                     .append(tableRow(["Started", self.parseTimeStamp(job[3]) + " (" + self.calcTimeDifference(job[3]) + " ago)"]));

                    var progress = self.makeProgressBarElement(job);
                    if (progress)
                        $infoTable.append(tableRow(["Progress", progress]));

                    $infoTable.append(tableRow(["Last Update", self.parseTimeStamp(job[5]) + " (" + self.calcTimeDifference(job[5]) + " ago)" ]));
                    if (job[11] !== 1 && job[10] !== 1)
                        $infoTable.append(tableRow(["Estimated Completion Time", self.parseTimeStamp(job[9]) + " (" + self.calcTimeDifference(job[9]) + ")"]));
                                     // .append(tableRow(["Progress", job[6]]))
                                     // .append(tableRow(["Max Progress", job[7]]))
                                     // .append(tableRow(["Progress Type", job[8]]))
                                     // .append(tableRow(["Complete?", job[10]]))
                                     // .append(tableRow(["Error?", job[11]]))
                                     // .append(tableRow(["Results", job[13]]));

                    var $modalBody = $("<div>")
                                     .append($infoTable);

                    // if there's an error let the user view what might be a stacktrace.
                    if (job[11] === 1) {

                    }

                    self.$elem.find(".modal .modal-dialog .modal-content .modal-body").html($modalBody);
                    self.$elem.find(".modal").modal();
                },

                self.clientError
            );
        },

        /**
         * @method clientError
         * Communicates a fairly simple error to the user when something goes wrong with a service.
         *
         * @param {object} error - the error object returned from a KBase service.
         * @private
         */
        clientError: function(error) {
            console.debug(error);
        },

        /**
         * @method parseTimeStamp
         * Parses the user_and_job_state timestamp and returns it as a user-
         * readable string in the UTC time.
         *
         * This assumes that the timestamp string is in the following format:
         * 
         * YYYY-MM-DDThh:mm:ssZ, where Z is the difference
         * in time to UTC in the format +/-HHMM, eg:
         *   2012-12-17T23:24:06-0500 (EST time)
         *   2013-04-03T08:56:32+0000 (UTC time)
         * 
         * If the string is not in that format, this method returns the unchanged
         * timestamp.
         *        
         * @param timestamp - the timestamp string returned by the service
         * @private
         */
        parseTimeStamp: function(timestamp) {
            var d = new Date(timestamp);

            if (!d)
                return timestamp;

            var addLeadingZeroes = function(value) {
                value = String(value);
                if (value.length === 1)
                    return "0" + value;
                return value;
            };

            return d.getFullYear() + "-" + 
                   addLeadingZeroes((d.getMonth() + 1)) + "-" + 
                   addLeadingZeroes(d.getDate()) + " " + 
                   addLeadingZeroes(d.getHours()) + ":" + 
                   addLeadingZeroes(d.getMinutes()) + ":" + 
                   addLeadingZeroes(d.getSeconds());
        },

        /**
         * @method calcTimeDifference
         * From two timestamps (i.e. Date.parse() parseable), calculate the
         * time difference and return it as a human readable string.
         *
         * @param {string} finish - the (estimated) finish timestamp
         * @return a string representing the time difference between the two parameter strings
         */
        calcTimeDifference: function(finish) {

            // start with seconds
            var timeRem = (Math.abs(new Date(finish) - new Date()) / 1000 );
            var unit = " sec";

            // if > 60 seconds, go to minutes.
            if (timeRem >= 60) {
                timeRem /= 60;
                unit = " min";
            }

            // if > 60 minutes, go to hours.
            if (timeRem >= 60) {
                timeRem /= 60;
                unit = " hrs";
            }

            // if > 24 hours, go to days
            if (timeRem >= 24) {
                timeRem /= 24;
                unit = " days";
            }

            // now we're in days. if > 364.25, go to years)
            if (timeRem >= 364.25) {
                timeRem /= 364.25;
                unit = " yrs";

                // now we're in years. just for fun, if we're over a century, do that too.
                if (timeRem >= 100) {
                    timeRem /= 100;
                    unit = " centuries";
                }
    
                // ok, fine, i'll do millennia, too.
                if (timeRem >= 10) {
                    timeRem /= 10;
                    unit = " millennia";
                }
            }

            return "~" + timeRem.toFixed(1) + unit;
        },

        /**
         * Makes a random-ish UUID.
         *
         * @private
         */
        uuid: function() {
            return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
                return v.toString(16);
            });
        }
    });
});