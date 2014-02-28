define(['jquery', 'kbwidget', 'kbaseAuthenticatedWidget', 'kbaseAccordion', 'kbasePrompt', 'bootstrap', 'userandjobstate', 'dataTables.bootstrap'], function($) {
    /**
     * @module kbaseUserJobState
     *
     * A reasonably simple widget to demonstrate the user_and_job_state service.
     * 
     * This widget follows the general format of the KBase Functional Site's user page (or 'My Stuff' page), implementing
     * a "Job Status" table view. It refreshes this table every minute (parameterized) as well.
     *
     * List of available options:
     * auth - the auth token.
     * userJobStateURL - location of the user_and_job_state service.
     * shockURL - base URL for shock (not including nodes)
     * workspaceURL - baseURL for the workspace browser
     * refreshTime - how often to refresh the statuses (in milliseconds)
     * detailRefreshTime - how often to refresh the job details popup (in milliseconds)
     * 
     * @exports kbaseUserJobState
     * @version 0.1
     * @author Bill Riehl <wjriehl@lbl.gov>
     */
    $.KBWidget({
        name: "kbaseUserJobState",
        parent: "kbaseAuthenticatedWidget",
        options: {
            loadingImage: "assets/images/ajax-loader-dark-blue.gif",
            errorLoadingImage: "assets/external/kbase/images/ajax-loader.gif",
//            userJobStateURL: "http://140.221.84.180:7083",
            userJobStateURL: "https://kbase.us/services/userandjobstate",
            shockURL: "http://kbase.us/services/shock/",
            workspaceURL: "http://kbase.us/services/workspace/",
            refreshTime: 60000,
            detailRefreshTime: 3000,
        },

        /**
         * @method init
         * Initializes this widget - called by the constructor.
         * @param {object} [options] - this optional object contains any options that need to be passed to the constructor. These include 'auth' an auth token and 'userJobStateURL' the URL of the user_and_job_state service.
         * @private
         */
        init: function(options) {
            this._super(options);
            /* Adds a timestamp sorting function.
             * Given that the elements given have an attribute called 'title' that
             * contains a time/date that's parseable by the Javascript Date object,
             * it'll sort 'em.
             */
            jQuery.fn.dataTableExt.oSort['timestamp-asc'] = function(x, y) {
                var ts1 = new Date($(x).attr('title'));
                var ts2 = new Date($(y).attr('title'));

                return ((ts1 < ts2) ? 1 : ((ts1 > ts2) ? -1 : 0));
            };

            jQuery.fn.dataTableExt.oSort['timestamp-desc'] = function(x, y) {
                var ts1 = new Date($(x).attr('title'));
                var ts2 = new Date($(y).attr('title'));

                return ((ts1 < ts2) ? -1 : ((ts1 > ts2) ? 1 : 0));
            };

            this.render();
            this.refresh();
            return this;
        },

        /**
         * @method render
         * This method renders the widget. It is called automatically during the initialization process.
         * The 'render' step here relates to the HTML rendering of the skeleton of what the widget should display.
         * The process of fetching job information and populating the widget is performed by the refresh() method.
         * @private
         */
        render: function() {
            // Initialize a blank message panel
            this.$messagePanel = $("<div>")
                                 .addClass("kbujs-loading")
                                 .hide();

            // Initialize the spinning loading image that sits in the header
            this.$loadingSpinner = $("<img src='" + this.options.loadingImage + "'>")
                                   .addClass("pull-right")
                                   .css("margin-top", "-3px")
                                   .hide();

            // Initialize a "loading please wait..." message
            this.$loadingMessage = $("<div>")
                                   .append($("<img src='" + this.options.loadingImage + "'>"))
                                   .append($("<div>")
                                           .append("Loading job status information...</div>"));

            // Define and track a loading image spinner.
            this.$loadingImage = $("<img>")
                                 .attr("src", this.options.loadingImage)
                                 .css({"display" : "none"});

            // Build the header info
            var $headerDiv = $('<div>')
                             .append('Job Status')
                             .append($('<button>')
                                     .addClass('btn btn-xs btn-default pull-right')
                                     .click($.proxy(function(event) { this.refresh(); }, this))
                                     .append($('<span>')
                                             .addClass('glyphicon glyphicon-refresh')))
                             .append(this.$loadingSpinner);

            // Define and track the table container (uses Bootstrap 3 and kbaseUserJobState.css)
            this.$jobSearch = $('<input>')
                              .attr({
                                 'type' : 'text',
                                 'placeholder' : 'Search',
                              })
                              .addClass('form-control pull-right');

            this.$jobSearch.keyup($.proxy(function(event) {
                var value = this.$jobSearch.val();
                this.$jobTable.fnFilter(value);
            }, this));


            this.$jobTable = $("<table>")
                             .addClass("table table-striped table-bordered kbujs-jobs-table");

            this.$tableContainer = $("<div>")
                                   .addClass("kbujs-table-container")
                                   .append(this.$jobSearch)
                                   .append(this.$jobTable);


            // Build the Modal dropdown that will contain detailed job information.
            this.$jobDetailBody = $("<div>");
            this.$jobDetailTitle = $("<span>").append("Job Details");
            this.$modal = $("<div>").kbasePrompt({
                title : this.$jobDetailTitle,
                body : this.$jobDetailBody,
                controls : [
                    {
                        name : 'Close',
                        type : 'primary',
                        callback : function(e, $prompt) {
                            $prompt.closePrompt();
                        },
                    },
                ]
            });
            
            this.$modal.dialogModal().on('hide.bs.modal', $.proxy(function() {
                if (this.modalRefreshInterval)
                    clearInterval(this.modalRefreshInterval);
                }, this)
            );


            // Build the Modal dropdown that will let users delete a job.
            this.$deleteBody = $("<div>");
            this.$deleteModal = $("<div>").kbasePrompt({
                title : "Delete Job?",
                body : this.$deleteBody,
                controls : [
                    'cancelButton',
                    {
                        name : 'Delete Job',
                        type : 'danger',
                        callback : $.proxy(function(e, $prompt) {
                            $prompt.closePrompt();
                            this.deleteJob(this.$deleteBody.attr('job-id'));
                        }, this)
                    }
                ]
            });

            this.$elem.append($('<div>')
                              .addClass('panel panel-primary')
                              .append($('<div>')
                                      .addClass('panel-heading')
                                      .append($('<div>')
                                              .addClass('panel-title')
                                              .css({'text-align': 'center'})
                                              .append($headerDiv)))
                              .append($('<div>')
                                      .addClass('panel-body kb-narr-panel-body')
                                      .css('padding', '3px')
                                      .append(this.$messagePanel)
                                      .append(this.$tableContainer)));

            this.$jobTable = this.$jobTable.dataTable({
                "oLanguage" : {
                    "sZeroRecords" : "<div style='text-align: center'>No jobs found</div>",
                },
                "bLengthChange" : false,
                "iDisplayLength": 20,
                "aoColumns" : [
                    { "sTitle" : "&nbsp;", "bSortable" : false, "bSearchable" : false },
                    { "sTitle" : "Service", "bSearchable" : true },
                    { "sTitle" : "Description", "bSearchable" : true },
                    { "sTitle" : "Started", "bSearchable" : true, "sType" : "timestamp" },
                    { "sTitle" : "Status", "bSearchable" : true },
                ],
                "sPaginationType" : "bootstrap",
                "aaSorting" : [[1, "asc"]],
                "aoColumnDefs" : [
                    {
                        "fnRender" : $.proxy(function( oObj ) {
                            return this.makePrettyTimestamp(oObj.aData[3], " ago");
                        }, this),
                        "aTargets": [3]
                    }
                ],
                "sDom": '<"top"ilp>rt<"bottom"><"clear">'
            });

            setInterval( 
                $.proxy(function() { this.refresh(); }, this), 
                this.options.refreshTime 
            );

            return this;
        },

        /**
         * @method loggedInCallback
         * This is associated with the login widget (through the kbaseAuthenticatedWidget parent) and
         * is triggered when a login event occurs.
         * It associates the new auth token with this widget and refreshes the data panel.
         * @private
         */
        loggedInCallback: function(event, auth) {
            this.token = auth;
            this.ujsClient = new UserAndJobState(this.options.userJobStateURL, this.token);
            this.isLoggedIn = true;
            this.refresh();
            return this;
        },

        /**
         * @method loggedOutCallback
         * Like the loggedInCallback, this is triggered during a logout event (through the login widget).
         * It throws away the auth token and workspace client, and refreshes the widget
         * @private
         */
        loggedOutCallback: function(event, auth) {
            this.token = null;
            this.ujsClient = null;
            this.isLoggedIn = false;
            this.refresh();
            return this;
        },


        /**
         * @method refresh
         * Refreshes the job status widget. If a user is logged in, then it fetches all of that user's jobs
         * from the user_and_job_state service, and populates the job status table with them.
         *
         * Otherwise, it prompts the user to log in.
         */
        refresh: function() {
            if (!this.token || this.token === null || !this.ujsClient) {
                this.showMessage("You must log in to view your jobs.");
                return;
            }

            else {
                this.$loadingSpinner.show();

                this.ujsClient.list_jobs([], '',
                    $.proxy(function(jobs) {
                        var jobTableRows = [];

                        for (var i=0; i<jobs.length; i++) {
                            var job = jobs[i];
                            jobTableRows.push([
                                "<button class='btn btn-primary btn-med' job-id='" + job[0] + "'><span class='glyphicon glyphicon-search'/></button>",
                                job[1],                         // service name
                                job[12],                        // description
                                job[3],
                                this.makeStatusElement(job),
                            ]);
                        }

                        this.$jobTable.fnClearTable();
                        this.$jobTable.off("click");

                        if (jobTableRows.length > 0) {
                            this.$jobTable.fnAddData(jobTableRows);

                            this.$jobTable.find("span[error-job-id]").click(
                                $.proxy(function(event) {
                                    this.showErrorDetails($(event.currentTarget).parent().attr('job-id'));
                                }, this)
                            );

                            this.$jobTable.find("button[job-id]").click(
                                $.proxy(function(event) {
                                    this.showJobDetails($(event.currentTarget).attr('job-id'));
                                }, this)
                            );

                            this.$jobTable.find("span.kbujs-delete-job").click(
                                $.proxy(function(event) {
                                    var jobId = $(event.currentTarget).parent().attr('job-id');
                                    this.$deleteBody.empty().append("Really delete this job? It'll be gone forever.");
                                    this.$deleteBody.attr('job-id', jobId);
                                    this.$deleteModal.openPrompt();
                                }, this)
                            );

                            $("[data-toggle='tooltip']").tooltip({'placement' : 'top'});
                            this.showJobTable();
                        }
                        else {
                            this.showMessage("No jobs found");
                        }
                        this.$loadingSpinner.hide();

                    }, this),

                    $.proxy(function(error) {
                        this.clientError(error);
                        this.$loadingSpinner.hide();
                    }, this)
                );
            }
        },

        deleteJob: function(jobId) {
            console.debug("Deleting job " + jobId);
            this.refresh();
            setTimeout($.proxy(function() {
                this.$deleteBody.html("Error! Unable to delete job!");
                this.$deleteModal.openPrompt();
            }, this), 1000);

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

            var timeHtml = "<div href='#' data-toggle='tooltip' title='" + parsedTime + "' class='kbujs-timestamp'>" + timediff + "</div>";
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
         * @private
         */
        makeStatusElement: function(job) {
            var status = "<div job-id='" + job[0] + "'>";

            if (job[11] === 1)
                status += "<span class='kbujs-error-cell kbujs-error' error-job-id='" + job[0] + "'>" +
                              "<span class='glyphicon glyphicon-exclamation-sign'></span>" +
                              "&nbsp;Error: " +
                              job[4] +
                          "</span>" +
                          "<span class='pull-right glyphicon glyphicon-remove kbujs-delete-job' data-toggle='tooltip' title='Delete Job'></span>";
            else if (job[10] === 1)
                status = "<div>Complete: " + job[4] + "</div>";
            else {
                status = "<div>" + job[4];
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
                    status += "</div>" + this.makeProgressBarElement(job, false);
                }

                if (job[9] != null) {
                    status += this.makePrettyTimestamp(job[9], " remaining");
                }
            }
            return status + "</div>";
        },

        /**
         * @method makeProgressBarElement
         * Makes a Bootstrap 3 Progress bar from the given job object.
         *
         * @param job - the job object
         * @param showNumber - if truthy, includes the numberical portion of what's being shown in the progressbar, separately.
         * @return A div containing a Bootstrap 3 progressbar, and, optionally, text describing the numbers in progress.
         * @private
         */
        makeProgressBarElement: function(job, showNumber) {
            var type = job[8].toLowerCase();
            var max = job[7] || 0;
            var progress = job[6] || 0;

            if (type === "percent") {
                var bar = "";
                if (showNumber)
                    bar += progress + "%";

                return bar + "<div class='progress' style='margin-bottom: 0;'>" + 
                           "<div class='progress-bar' role='progressbar' aria-valuenow='" + 
                               progress + "' aria-valuemin='0' aria-valuemax='100' style='width: " + 
                               progress + "%;'>" +
                               "<span class='sr-only'>" + progress + "% Complete" + "</span>" +
                           "</div>" +
                       "</div>";
            }
            else {
                var bar = "";
                if (showNumber)
                    bar += progress + " / " + max;
                return bar + "<div class='progress' style='margin-bottom: 0;'>" + 
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
            var self = this;

            self.$jobDetailTitle.html("Job Details");
            self.$modal.openPrompt();

            var tableRow = function(elems) {
                var row = $("<tr>");
                for (var i=0; i<elems.length; i++) {
                    row.append($("<td>").append(elems[i]));
                }
                return row;
            };

            var parseStage = function(stage) {
                if (stage.toLowerCase() === "error") {
                    var $btn = $("<span/>")
                               .addClass("kbujs-error")
                               .append($("<span/>")
                                       .addClass("glyphicon glyphicon-exclamation-sign"))
                               .append(" Error")
                               .click(function(event) {
                                    self.$modal.off('hidden.bs.modal');
                                    self.$modal.on('hidden.bs.modal', function(event) {
                                        self.showErrorDetails(jobId);
                                    });
                                    self.$modal.modal("hide");
                                });

                    return $("<div>")
                           .addClass("kbujs-error-cell")
                           .append($btn);
                }
                return stage;
            };

            var refresh = function() {
                self.ujsClient.get_job_info(jobId, 
                    function(job) {

                        var $infoTable = $("<table>")
                                         .addClass("table table-striped table-bordered")
                                         .append(tableRow(["Job ID", job[0]]))
                                         .append(tableRow(["Service", job[1]]))
                                         .append(tableRow(["Description", job[12]]))
                                         .append(tableRow(["Stage", parseStage(job[2])]))
                                         .append(tableRow(["Status", job[4]]))
                                         .append(tableRow(["Started", self.parseTimeStamp(job[3]) + " (" + self.calcTimeDifference(job[3]) + ")"]));

                        var progress = self.makeProgressBarElement(job, true);
                        if (progress)
                            $infoTable.append(tableRow(["Progress", progress]));

                        $infoTable.append(tableRow(["Last Update", self.parseTimeStamp(job[5]) + " (" + self.calcTimeDifference(job[5]) + ")" ]));
                        if (job[11] !== 1 && job[10] !== 1)
                            $infoTable.append(tableRow(["Estimated Completion Time", self.parseTimeStamp(job[9]) + " (" + self.calcTimeDifference(job[9]) + ")"]));

                        var $modalBody = $("<div>")
                                         .append($infoTable);

                        if (job[13]) {
                            var resultsData = false;

                            var $resultsDiv = $("<div>")
                                              .append("<h2>Job Complete</h2>");

                            var $resultsTable = $("<table>")
                                               .addClass("table table-striped table-bordered");

                            if (job[13].shocknodes && job[13].shocknodes.length > 0) {
                                var shockURL = self.options.shockURL;
                                if (job[13].shockurl)
                                    shockURL = job[13].shockurl;

                                var shockNodeUrls = [];
                                for (var i=0; i<job[13].shocknodes.length; i++) {
                                    shockNodeUrls.push("<a href='" + shockURL + job[13].shocknodes[i] + "' target='_blank'>" + job[13].shocknodes[i] + "</a>");
                                }
                                $resultsTable.append(tableRow(["Shock", shockNodeUrls.join("<br/>")]));

                                resultsData = true;
                            }

                            if (job[13].workspaceids && job[13].workspaceids.length > 0) {
                                var workspaceURL = self.options.workspaceURL;
                                if (job[13].workspaceurl)
                                    workspaceURL = job[13].workspaceurl;

                                var workspaceUrls = [];
                                for (var i=0; i<job[13].workspaceids.length; i++) {
                                    workspaceUrls.push("<a href='" + workspaceURL + job[13].workspaceids[i] + "' target='_blank'>" + job[13].workspaceids[i] + "</a>");
                                }
                                $resultsTable.append(tableRow(["Workspace", workspaceUrls.join("<br/>")]));
                                resultsData = true;
                            }

                            if (resultsData === true)
                                $resultsDiv.append($resultsTable);
                            $modalBody.append($resultsDiv);
                        }

                        // if there's an error let the user view what might be a stacktrace.
                        if (job[11] === 1) {

                        }
                        console.debug("refreshed!");
                        self.$jobDetailBody.html($modalBody);

                    },

                    $.proxy(function(error) { 
                        this.$modal.closePrompt();
                        this.clientError(error); 
                    }, self)
                );
            };

            refresh();
            self.modalRefreshInterval = setInterval( function() { refresh(); }, this.options.detailRefreshTime );
        },

        /**
         * Builds and shows a detailed error panel, including a stacktrace, if applicable, for a Job.
         *
         * This assumes that a job has an error.
         */
        showErrorDetails: function(jobId) {
            this.$modal.off('hidden.bs.modal');
            this.$modal.closePrompt();
            this.$jobDetailTitle.html("Job Error");

            this.$jobDetailBody.html("<div class='kbujs-loading-modal'><img src='" + this.options.errorLoadingImage + "'/><br/>Loading...</div>");

            this.$modal.openPrompt();

            this.ujsClient.get_job_info(jobId,
                $.proxy(function(job) {
                    var $table = $("<table>")
                                 .addClass("table table-striped table-bordered")
                                 .append("<tr><td>Job ID</td><td>" + job[0] + "</td></tr>")
                                 .append("<tr><td>Service</td><td>" + job[1] + "</td></tr>")
                                 .append("<tr><td>Description</td><td>" + job[12] + "</td></tr>")
                                 .append("<tr><td>Status</td><td>" + job[4] + "</td></tr>");

                    this.ujsClient.get_detailed_error(jobId,
                        $.proxy(function(error) {
                            var $detailedError = $("<div>").append("<h3>Error Details</h3>");
                            if (error && error.length > 0)
                                $detailedError.append($("<pre>").append(error));
                            else
                                $detailedError.append("No error information found.");
                            this.$jobDetailBody.empty()
                                               .append($table)
                                               .append($detailedError);
                        }, this),

                        $.proxy(function(apiError) {
                            this.$modal.closePrompt();
                            this.clientError(apiError);
                        }, this)
                    );

                }, this),

                $.proxy(function(error) {
                    this.$modal.closePrompt();
                    this.clientError(error);
                }, this)
            );
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
         * @param {String} timestamp - the timestamp string returned by the service
         * @returns {String} a parsed timestamp in the format "YYYY-MM-DD HH:MM:SS" in the browser's local time.
         * @private
         */
        parseTimeStamp: function(timestamp) {
            var d = this.parseDate(timestamp);
            if (d === null)
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
         * @param {String} time - the timestamp to calculate a difference from
         * @returns {String} - a string representing the time difference between the two parameter strings
         */
        calcTimeDifference: function(time) {
            var now = new Date();
            time = this.parseDate(time);

            if (time === null)
                return "Unknown time";

            // start with seconds
            var timeRem = Math.abs((time - now) / 1000 );
            var unit = " sec";

            // if > 60 seconds, go to minutes.
            if (timeRem >= 60) {
                timeRem /= 60;
                unit = " min";

                // if > 60 minutes, go to hours.
                if (timeRem >= 60) {
                    timeRem /= 60;
                    unit = " hrs";

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

                            // ok, fine, i'll do millennia, too.
                            if (timeRem >= 10) {
                                timeRem /= 10;
                                unit = " millennia";
                            }
                        }
                    }
                }
            }


            var timediff = "~" + timeRem.toFixed(1) + unit;
            if (time > now)
                timediff += " from now";
            else
                timediff += " ago";

            return timediff;
        },

        /**
         * Hides the job info table and shows the given message (as an HTML object or jQuery node)
         */
        showMessage: function(message) {
            this.$tableContainer.hide();
            this.$messagePanel.empty().append(message);
            this.$messagePanel.show();
        },

        /**
         * Hides the message panel and shows the job table.
         */
        showJobTable: function() {
            this.$messagePanel.hide();
            this.$tableContainer.show();
        },

        /**
         * @method clientError
         * Communicates a fairly simple error to the user when something goes wrong with a service.
         *
         * @param {object} error - the error object returned from a KBase service.
         * @private
         */
        clientError: function(error) {
            var $errorDiv = $("<div>");

            var $errorHeader = $('<div>')
                               .addClass('alert alert-danger')
                               .append('<b>Sorry, an error occurred while loading KBase job information.</b>')
                               .append('<br>Please contact the KBase team at <a href="mailto:help@kbase.us?subject=KBase%20Job%20Service%20Error">help@kbase.us</a> with the information below.');

            var $errorMessage = $('<div>');

            // If it's a string, just dump the string.
            if (typeof error === 'string') {
                $errorMessage.append($('<div>').append(error));
            }

            // If it's an object, expect an error object as returned by the execute_reply callback from the IPython kernel.
            else if (typeof error === 'object') {
                var $details = $('<div>');
                $details.append($('<div>').append('<b>Type:</b> ' + error.ename))
                        .append($('<div>').append('<b>Value:</b> ' + error.evalue));

                if (error.traceback && error.traceback.length > 0) {
                    var $tracebackDiv = $('<div>')
                                     .addClass('kb-function-error-traceback');
                    for (var i=0; i<error.traceback.length; i++) {
                        $tracebackDiv.append(error.traceback[i] + "<br>");
                    }

                    var $tracebackPanel = $('<div>');
                    var tracebackAccordion = [{'title' : 'Traceback', 'body' : $tracebackDiv}];

                    this.$errorMessage.append($details)
                                      .append($tracebackPanel);
                    $tracebackPanel.kbaseAccordion({ elements : tracebackAccordion });
                }
                else {
                    $errorHeader.html('<b>Sorry, an unknown error occurred while loading KBase job information.</b>' +
                                      '<br>If this error persists, please contact the KBase team at ' +
                                      '<a href="mailto:help@kbase.us?subject=Unknown%20KBase%20Job%20Service%20Error">help@kbase.us</a>');
                }
            }
            $errorDiv.append($errorHeader)
                     .append($errorMessage);
            this.showMessage($errorDiv);
        },

        /**
         * VERY simple date parser.
         * Returns a valid Date object if that time stamp's real. 
         * Returns null otherwise.
         * @param {String} time - the timestamp to convert to a Date
         * @returns {Object} - a Date object or null if the timestamp's invalid.
         */
        parseDate: function(time) {
            var d = new Date(time);
            if (Object.prototype.toString.call(d) === '[object Date]') {
                if (isNaN(d.getTime())) {
                    return null;
                }
                else {
                    return d;
                }
            }
            return null;
        },
    });
});
