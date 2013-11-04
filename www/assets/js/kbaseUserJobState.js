define(['jquery', 'kbwidget', 'bootstrap', 'userandjobstate', 'jquery.dataTables'], function($) {
    $.KBWidget({
        name: "kbaseUserJobState",
        options: {
            auth: null,
            loadingImage: "assets/external/kbase/images/ajax-loader-blue.gif",
            userJobStateURL: "http://140.221.84.180:7083",
        },

        init: function(options) {
            this._super(options);

            this.render();
            this.refresh();
            return this;
        },

        render: function() {
            var self = this;

            this.$loadingImage = $("<img>")
                                 .attr("src", this.options.loadingImage)
                                 .css({"display" : "none"});

            this.$refreshButton = $("<a>")
                                  .addClass("btn btn-primary btn-sm glyphicon glyphicon-refresh")
                                  .click(function(event) {
                                      self.refresh();
                                  });

            this.$tableContainer = $("<div>")
                                   .addClass("row in kbujs-table-container");

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

            var $container = $("<div>")
                             .addClass("table_section container")
                             .append($("<div>")
                                     .addClass("section_title row")
                                     .append($("<div>")
                                             .addClass("title col-md-2")
                                             .append($("<a>")
                                                     .attr("href", "#")
                                                     .attr("data-toggle", "collapse")
                                                     .attr("data-target", "#job_status_container")
                                                     .append($("<span>")
                                                             .addClass("glyphicon glyphicon-chevron-down"))
                                                     .append("&nbsp;Job Status")
                                                     .append(this.$loadingImage)))
                                     .append($("<div>")
                                             .addClass("col-md-1 col-md-offset-9 pull-right")
                                             .append(this.$refreshButton)))
                             .append(this.$tableContainer);

            this.$elem.append($container);
            this.$elem.append(this.$modal);
            return this;
        },

        setAuth: function(token) {
            console.log("setting token");
            console.log(token);
            if (token)
                this.options.auth = token;
            else {
                this.options.auth = null;
                this.userJobStateClient = null;
            }

            this.refresh();
        },

        refresh: function() {
            if (this.$jobTable) {
                this.$jobTable.off("click");
                this.$jobTable.fnDestroy();
                this.$jobTable.remove();
            }
            if (!this.options.auth) {
                this.$tableContainer.html("You are not logged in. Please log in to view your jobs.");
                return;
            }

            this.$loadingImage.css({"display": ""});
            this.userJobStateClient = new UserAndJobState(this.options.userJobStateURL, this.options.auth);
            var self = this;

            this.userJobStateClient.list_job_services(
                function(services) {

                    var getServiceJobs = [];
                    var allJobsList = [];

                    $.each(services, function(index, name) {
                        getServiceJobs.push(self.userJobStateClient.list_jobs(name, '', 
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
                                job[1],     // service name
                                job[2],     // stage
                                job[4],     // status
                                job[3],     // started
                                job[9],     // est complete
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
                                    { "sTitle" : "&nbsp;" },
                                    { "sTitle" : "Service" },
                                    { "sTitle" : "Stage" },
                                    { "sTitle" : "Status" },
                                    { "sTitle" : "Started" },
                                    { "sTitle" : "Time Remaining" },
                                ],
                                "sPaginationType" : "full_numbers",
                                "aaSorting" : [[0, "asc"]],
                                "aoColumnDefs" : [
                                ]
                            });

                            self.$jobTable.on("click", "button[job-id]",
                                function(event) { 
                                    self.showJobDetails($(event.currentTarget).attr('job-id'));
                                }
                            );

                            self.$tableContainer.append(self.$jobTable);
                        }
                        else {
                            self.$tableContainer.append("You have no running jobs.");
                        }
                        self.$loadingImage.css({ 'display' : 'none' });

                    });
                },
                this.clientError
            );
        },

        /**
         * Shows the details of the job with the given ID in a modal dialog.
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
                    var $modalBody = $("<table>")
                                     .addClass("table table-striped table-bordered")
                                     .append(tableRow(["Job ID", job[0]]))
                                     .append(tableRow(["Service", job[1]]))
                                     .append(tableRow(["Stage", job[2]]))
                                     .append(tableRow(["Started", job[3]]))
                                     .append(tableRow(["Status", job[4]]))
                                     .append(tableRow(["Last Update", job[5]]))
                                     .append(tableRow(["Progress", job[6]]))
                                     .append(tableRow(["Max Progress", job[7]]))
                                     .append(tableRow(["Progress Type", job[8]]))
                                     .append(tableRow(["Estimated Completion Time", job[9]]))
                                     .append(tableRow(["Complete?", job[10]]))
                                     .append(tableRow(["Error?", job[11]]))
                                     .append(tableRow(["Description", job[12]]))
                                     .append(tableRow(["Results", job[13]]));


                    self.$elem.find(".modal .modal-dialog .modal-content .modal-body").html($modalBody);
                    self.$elem.find(".modal").modal();

                },

               self.clientError
            );
        },

        /**
         * Communicates a fairly simple error to the user when something goes wrong with a service.
         *
         * @private
         */
        clientError: function(error) {
            console.debug(error);
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