(ns sixsq.nuvla.ui.i18n.dictionary
  ;; the moment.js locale must be included for any supported language
  (:require
    ["moment/locale/fr"]))


(def dictionary
  {:en {:lang                                      "english"

        :about                                     "about"
        :about-subtitle                            "This software is brought to you"
        :access-deployment                         "access deployment"
        :acl-owners                                "Owners has all rights on the resource. This is why they don't appear in rights table."
        :acl-rights-delete                         "Allow to delete the resource. It implies the view-meta right on the resource only visible in advanced mode."
        :acl-rights-edit                           "Allow to edit all fields of the resource. The edit implies all rights, it's equivalent to be one of the owners of the resource. Implied rights can't be unselected."
        :acl-rights-edit-acl                       "Edit-acl implies all rights. It's equivalent to be one of the owners of the resource."
        :acl-rights-edit-data                      "Allow to edit all fields other than acl."
        :acl-rights-edit-meta                      "Allow to edit metadata fields as e.g. name, description, tags."
        :acl-rights-manage                         "Allow to use defined action on the resource."
        :acl-rights-view                           "Allow to view all fields of the resource. Implied rights can't be unselected."
        :acl-rights-view-acl                       "Allow to view all fields of the resource."
        :acl-rights-view-data                      "View-data allow to view all fields other than acl."
        :acl-rights-view-meta                      "View-meta allow you to view metadata fields as name, description, tags, id, resource-type, created, updated, tags, parent, resource-metadata, operations."
        :actions                                   "actions"
        :active?                                   "active only?"
        :add                                       "add"
        :aggregation                               "aggregation"
        :all-credentials                           "all credentials"
        :all-projects                              "all projects"
        :all-users                                 "all users"
        :already-registered?                       "Already registered?"
        :and                                       "and"
        :api                                       "api"
        :api-doc                                   "API documentation"
        :application                               "application"
        :apps                                      "apps"
        :appstore                                  "app store"
        :are-you-sure?                             "Are you sure?"
        :attributes                                "attributes"
        :billable-only?                            "billable only?"
        :ca                                        "public certificate of the Certificate Authority (CA)"
        :cert                                      "client's public certificate"
        :cancel                                    "cancel"
        :change-password                           "change password"
        :cimi                                      "api"
        :clear                                     "clear"
        :clear-all                                 "clear all"
        :click-for-depl-details                    "click on the card for deployment details"
        :close                                     "close"
        :cloud                                     "cloud"
        :columns                                   "columns"
        :coming-soon                               "Coming soon"
        :commit-placeholder                        "Commit message - explicit is better"
        :component                                 "component"
        :configure                                 "configure"
        :core-license                              "Core license"
        :count                                     "count"
        :create                                    "create"
        :created                                   "created"
        :credentials                               "credentials"
        :credentials-cloud-services                "Credentials for Cloud Services"
        :credential-delete-warning                 "I understand that deleting this credential is permanent and cannot be undone. This action might affect services started or created with this credential."
        :credentials-help                          "Credentials allow you to access clouds and infrastructure services."
        :credentials-infra-services                "Credentials for Infrastructure Services"
        :credentials-sub-text                      "Credentials allow you to authenticate with the different cloud and infrastructure services Nuvla manages for you."
        :credential-cloud-section                  "Cloud Services"
        :credential-infra-service-section          "Infrastructure Services"
        :credential-infra-service-section-sub-text "Credentials used to manage infrastructure services, such as Swarm, MinIO and storage service from cloud providers, such as Amazon, Azure and Exoscale"
        :current-user                              "current user"
        :current-password                          "current password"
        :custom                                    "custom"
        :dashboard                                 "dashboard"
        :data                                      "data"
        :data-binding                              "Data Binding"
        :data-type                                 "data type"
        :delete                                    "delete"
        :delete-resource                           "delete resource"
        :delete-resource-msg                       "delete resource %1?"
        :delta-min                                 "delta [min]"
        :deployment                                "deployment"
        :describe                                  "describe"
        :description                               "description"
        :details                                   "details"
        :documentation                             "documentation"
        :download                                  "download"
        :drop-file                                 "drop file"
        :edit                                      "edit"
        :editing                                   "editing"
        :error                                     "error"
        :event                                     "event"
        :events                                    "events"
        :execute-action                            "execute action"
        :execute-action-msg                        "execute action %1?"
        :fields                                    "fields"
        :filter                                    "filter"
        :first                                     "first"
        :forgot-password                           "Forgot your password?"
        :from                                      "from"
        :global-parameters                         "global parameters"
        :groups                                    "groups"
        :hide-versions                             "hide versions"
        :id                                        "id"
        :ignore-changes                            "ignore changes"
        :ignore-changes?                           "Ignore changes?"
        :ignore-changes-content                    "This page contains unsaved changes. Are you sure you want to loose these changes?"
        :image                                     "image"
        :infra-services                            "Infrastructure Services"
        :infra-service-short                       "infrastructures"
        :input-parameters                          "input parameters"
        :job                                       "job"
        :key                                       "client's private certificate"
        :knowledge-base                            "knowledge base"
        :last                                      "last"
        :last-30-days                              "last 30 days"
        :last-7-days                               "last 7 days"
        :launch                                    "launch"
        :less                                      "less"
        :less-details                              "less details"
        :limit                                     "limit"
        :loading                                   "loading"
        :login                                     "login"
        :login-failed                              "login failed"
        :login-link                                "Login."
        :logo-url-placeholder                      "e.g. http://example.com/images/logo.png"
        :logout                                    "logout"
        :manage                                    "manage"
        :message                                   "message"
        :messages                                  "messages"
        :module                                    "module"
        :module-change-logo                        "Change logo"
        :module-data-type-help                     "..."
        :module-docker-command-message             "You can test this component by running the following command locally:"
        :module-docker-name-help                   "Docker image name"
        :module-docker-image-label                 "docker image"
        :module-docker-image-placeholder           "image - e.g. ubuntu"
        :module-docker-registry-placeholder        "hub.docker.com"
        :module-docker-tag-placeholder             "latest"
        :module-docker-repository-placeholder      "org. - e.g. ubuntu"
        ;:module-force-image-help         "Force pull"
        :module-restart-policy                     "Restart policy"
        ;:module-force-pull-image?        "Force image pull?"
        :module-output-parameters                  "Output Parameters"
        :module-output-parameters-help             "output parameters - ..."
        :module-ports-published-port-placeholder   "source - e.g. 22 or 22-23"
        :module-ports-target-port-placeholder      "dest. - e.g. 22 or 22-23"
        :module-ports                              "Port Mappings"
        :module-ports-help                         "Definitions of port mappings between the container and its host - e.g. \"tcp:22:22\", \"tcp::80\", \"tcp:8022-8023:2222-2223\""
        :module-publish-port                       "Publish ports"
        :module-restart-policy-help                "Policy when a container fails (i.e. exits)"
        :module-urls-help                          "URLs to help access the services"
        :module-mounts                             "Volumes (mounts)"
        :module-mount-read-only?                   "Read only?"
        :module-mount-help                         "Volumes (or mounts) definitions for the container. You can define custom type and driver"
        :module-mount-section-desc                 "Container volumes (i.e. mounts) "
        :modules                                   "modules"
        :more                                      "more"
        :more-details                              "details"
        :more-info                                 "More information..."
        :name                                      "name"
        :new-component                             "new component"
        :new-password                              "new password"
        :new-password-repeat                       "repeat your password"
        :new-project                               "new project"
        :next-step                                 "next step"
        :no                                        "no"
        :no-account?                               "No account?"
        :no-apps                                   "no matching applications"
        :no-children-modules                       "no sub-projects"
        :no-credentials                            "no credentials for selected infrastructure"
        :no-data                                   "no data"
        :no-data-location                          "no location with selected data"
        :no-datasets                               "no data binding defined for this application"
        :no-input-parameters                       "no input parameters defined for the application"
        :no-messages                               "no messages"
        :no-mounts                                 "no volumes or mounts for this applications"
        :no-output-parameters                      "no output parameters defined for the application"
        :no-ports                                  "no port mapping defined for this application"
        :no-urls                                   "no URLs for this application"
        :notifications                             "notifications"
        :nuvlabox                                  "NuvlaBox"
        :nuvlabox-ctrl                             "edge control"
        :object-count                              "Number of data objects: %1"
        :objects                                   "objects"
        :offset                                    "offset"
        :order                                     "order"
        :owners                                    "owners"
        :parameters                                "parameters"
        :parent                                    "parent"
        :password                                  "password"
        :password-updated                          "password updated"
        :password-repeat                           "repeat password"
        :personae-desc                             "Personae description"
        :preview                                   "preview"
        :previous-step                             "previous step"
        :principals-icon                           "Principals icon"
        :process                                   "process"
        :product-info                              "product information"
        :profile                                   "profile"
        :progress                                  "progress"
        :project                                   "project"
        :raw                                       "raw"
        :refresh                                   "refresh"
        :release-notes                             "release notes"
        :reports                                   "reports"
        :reset-password                            "reset password"
        :reset-password-inst                       "Enter your username to reset your password. We'll send an email with instructions."
        :reset-password-error                      "Error resetting password"
        :resource-type                             "resource type"
        :results                                   "results"
        :return-code                               "return code"
        :rights                                    "rights"
        :rights-icon                               "Rights icon"
        :save                                      "save"
        :search                                    "search"
        :select                                    "select"
        :select-application                        "select application"
        :select-datasets                           "click on cards to select or deselect dataset(s)"
        :select-file                               "select file"
        :select-logo-url                           "Select logo URL"
        :session                                   "current session"
        :session-expires                           "session expires"
        :settings                                  "settings"
        :show-versions                             "show versions"
        :signup                                    "sign up"
        :signup-failed                             "sign up failed"
        :signup-link                               "Sign up."
        :size                                      "size"
        :source-code-on                            "source code on"
        :start                                     "start"
        :state                                     "state"
        :statistics                                "statistics"
        :status                                    "status"
        :stop                                      "stop"
        :success                                   "Success"
        :summary                                   "summary"
        :support                                   "support"
        :tags                                      "tags"
        :tech-doc                                  "technical documentation"
        :terminate                                 "terminate"
        :timestamp                                 "timestamp"
        :to                                        "to"
        :today                                     "today"
        :total                                     "total"
        :tutorials                                 "tutorials"
        :type                                      "type"
        :unauthorized                              "unauthorized"
        :update                                    "update"
        :url                                       "URL"
        :urls                                      "URLs"
        :username                                  "username"
        :users                                     "users"
        :validation-error                          "Validation error!"
        :validation-error-message                  "The form in invalid. Please review the fields in red."
        :validation-email-success-msg              "A validation message has been sent to your email account."
        :version-number                            "Version number"
        :value                                     "value"
        :view                                      "view"
        :vms                                       "VMs"
        :volumes                                   "Volumes"
        :welcome                                   "welcome"
        :welcome-api-desc                          "Explore and play with our rich API"
        :welcome-application-desc                  "Manage apps, components and images. Collaborate, share and explore one-click deployable applications"
        :welcome-appstore-desc                     "Browse apps, components and images published and shared"
        :welcome-detail                            "Complete solution to manage your multi-cloud to edge continuum"
        :welcome-dashboard-desc                    "One glance to understand at once all that matters"
        :welcome-data-desc                         "Browse datasets and launch data analytics"
        :welcome-deployment-desc                   "See and control all your apps, across all clouds and edge"
        :welcome-docs-desc                         "Learn about the API resources using the API documentation."
        :welcome-nuvlabox-desc                     "Add IoT and edge management. Control all your NuvlaBoxes from one place"
        :yes                                       "yes"
        :yesterday                                 "yesterday"}

   :fr {:lang                            "français"

        :about                           "à propos"
        :about-subtitle                  "Ce logiciel vous est fournit"
        :access-deployment               "accéder déploiement"
        :acl-owners                      "Les propriétaires ont tous les droits sur la ressources. C'est pour cette raison qu'ils n'apparaissent pas dans la table des droits."
        :acl-rights-delete               "Permet d'effacer la ressource. Ce droit implique le droit view-meta sur la ressources qui est visible uniquement en mode avancé."
        :acl-rights-edit                 "Permet d'éditer tous les champs de la ressource. Cette permissions implique tous les autres droits et donc permet un niveau de droits équivalent aux propriétaires de la ressource. Les droits impliqués ne peuvent être désélectionnés."
        :acl-rights-edit-acl             "Edit-acl implique tous les droits et donc permet un niveau de droits équivalent aux propriétaires de la ressource"
        :acl-rights-edit-data            "Edit-data permet d'éditer tous les champs à part le champ acl."
        :acl-rights-edit-meta            "Edit-meta permet d'éditer les champs métadonnées. Par example : name, description, tags."
        :acl-rights-manage               "Permet d'utiliser les actions définis sur la ressource."
        :acl-rights-view                 "Permet de voir tous les champs de la ressource. Les droits impliqués ne peuvent être désélectionnés."
        :acl-rights-view-acl             "Permet de voir tous les champs de la ressource."
        :acl-rights-view-data            "Permet de voir tous les champs autres que acl."
        :acl-rights-view-meta            "Permet de voir les champs métadonnés suivant:  name, description, tags, id, resource-type, created, updated, tags, parent, resource-metadata, operations."
        :actions                         "actions"
        :active?                         "uniquement actif ?"
        :add                             "ajouter"
        :aggregation                     "aggréger"
        :all-users                       "tous les utilisateurs"
        :all-credentials                 "toutes les informations d'identification"
        :all-projects                    "tous les projets"
        :already-registered?             "Déjà enregistré ?"
        :and                             "et"
        :api                             "api"
        :api-doc                         "Documentation de l'API"
        :application                     "application"
        :apps                            "apps"
        :appstore                        "boutique d'applications"
        :are-you-sure?                   "Êtes-vous sûr ?"
        :attributes                      "attributs"
        :billable-only?                  "facturable seulement ?"
        :cancel                          "annuler"
        :change-password                 "changer de mot de passe"
        :cimi                            "api"
        :close                           "fermer"
        :cloud                           "nuage"
        :clear                           "effacer"
        :clear-all                       "tout effacer"
        :click-for-depl-details          "cliquez sur une carte pour afficher le détail du déploiement"
        :columns                         "colonnes"
        :commit-placeholder              "Message d'enregistrement - un message clair c'est mieux"
        :component                       "composant"
        :configure                       "configurer"
        :core-license                    "license de base"
        :count                           "décompte"
        :credentials                     "informations d'identification"
        :current-user                    "utilisateur actuel"
        :current-password                "mot de passe actuel"
        :create                          "créer"
        :created                         "créé"
        :custom                          "personnalisé"
        :dashboard                       "tableau de bord"
        :data                            "données"
        :data-binding                    "Couplage de Données"
        :data-type                       "type de données"
        :delete                          "supprimer"
        :delete-resource                 "supprimer ressource"
        :delete-resource-msg             "supprimer ressource %1 ?"
        :delta-min                       "delta [min]"
        :deployment                      "déploiement"
        :describe                        "décrire"
        :description                     "description"
        :details                         "détails"
        :documentation                   "documentation"
        :download                        "télécharger"
        :drop-file                       "déposer un fichier"
        :edit                            "modifier"
        :editing                         "modification en cours"
        :error                           "erreur"
        :event                           "événement"
        :events                          "événements"
        :execute-action                  "exécuter la tâche"
        :execute-action-msg              "exécuter la tâche %1 ?"
        :fields                          "champs"
        :filter                          "filtre"
        :first                           "début"
        :forgot-password                 "Mot de passe oublié?"
        :from                            "de"
        :global-parameters               "paramètres globaux"
        :groups                          "groupes"
        :hide-versions                   "cacher versions"
        :id                              "id"
        :ignore-changes?                 "Ignorer les changements?"
        :ignore-changes-content          "Cette page contient des changements qui n'ont pas été sauvegardés. Etes-vous certain de vouloir les perdre?"
        :image                           "image"
        :infra-services                  "Services d<Infrastructure"
        :infra-service-short             "infrastructures"
        :input-parameters                "paramètres d'entrée"
        :job                             "tâche"
        :knowledge-base                  "base de connaissance"
        :last                            "fin"
        :last-30-days                    "derniers 30 jours"
        :last-7-days                     "derniers 7 jours"
        :launch                          "lancer"
        :less                            "moins"
        :less-details                    "moins de details"
        :limit                           "limite"
        :loading                         "chargement en cours"
        :login                           "se connecter"
        :login-failed                    "la connexion a échoué"
        :login-link                      "Se connecter."
        :logo-url-placeholder            "p.ex.: http://example.com/images/logo.png"
        :logout                          "déconnexion"
        :manage                          "gérer"
        :message                         "message"
        :messages                        "messages"
        :module                          "module"
        :module-change-logo              "Changer le logo"
        :module-data-type-help           "..."
        :module-docker-name-help         "image Docker"
        :module-docker-image-label       "image docker"
        :module-docker-image-placeholder "p.ex.: ubuntu:18.11"
        ;:module-force-image-help         "Forcer le téléchargement"
        :module-restart-policy           "Politique de redémarrage"
        ;:module-force-pull-image?        "Forcer le téléchargement?"
        :module-output-parameters        "paramètres de sortie"
        :module-output-parameters-help   "paramètres de sortie - ..."
        :module-ports                    "Redirection de ports"
        :module-ports-help               "Definitions de la redirection de ports entre le container et son hôte - p.ex.: \"tcp:22:22\", \"tcp::80\", \"tcp:8022-8023:2222-2223\""
        :module-publish-port             "Publication des ports"
        :module-restart-policy-help      "Politique en cas d'erreur (i.e. sortie/exit)"
        :module-urls-help                "URLs pour mieux retrouver les services"
        :module-mounts                   "Volumes (\"mounts\")"
        :module-mount-read-only?         "Lecture seule?"
        :module-mount-help               "Définitions des volumes (ou 'mounts') du container. Vous pouvez définir le type et le pilot ('driver')"
        :module-mount-section-desc       "Volume pour les containers (i.e. `mounts`) "
        :modules                         "modules"
        :more                            "plus"
        :more-details                    "détails"
        :more-info                       "Plus d'informations"
        :name                            "nom"
        :new-component                   "nouveau composant"
        :new-password                    "nouveau mot de passe"
        :new-password-repeat             "repétez votre mot de passe"
        :new-project                     "nouveau projet"
        :next-step                       "étape suivante"
        :no                              "non"
        :no-account?                     "Pas de compte ?"
        :no-apps                         "pas d'applications correspondantes"
        :no-children-modules             "pas de sous-projet"
        :no-credentials                  "pas d'informations d'identifcation pour l'infrastructure sélectionné"
        :no-data                         "pas de données"
        :no-data-location                "aucun lieu contien les données sélectionnées"
        :no-datasets                     "pas de collection de données"
        :no-input-parameters             "aucun paramètre d'entrée définié pour l'application"
        :no-messages                     "aucun message"
        :no-mounts                       "aucun volume (\"mount\") pour cette applications"
        :no-output-parameters            "aucun paramètre de sortie définié pour l'application"
        :no-ports                        "aucune redirection de port n'est définie pour cette application"
        :notifications                   "notifications"
        :nuvlabox                        "NuvlaBox"
        :nuvlabox-ctrl                   "contrôle de bord"
        :object-count                    "Nombre d'objets de données : %1"
        :objects                         "objets"
        :offset                          "décalage"
        :order                           "ordonner"
        :owners                          "propriétaires"
        :parameters                      "paramètres"
        :parent                          "parent"
        :password                        "mot de passe"
        :password-updated                "votre mot de passe a éte mis à jour"
        :password-repeat                 "repétez le mot de passe"
        :personae-desc                   "Description des acteurs"
        :preview                         "aperçu"
        :previous-step                   "étape précédente"
        :principals-icon                 "Icône représentant les responsables"
        :process                         "traiter"
        :product-info                    "information produit"
        :profile                         "profil utilisateur"
        :progress                        "progression"
        :project                         "projet"
        :raw                             "brute"
        :refresh                         "actualiser"
        :release-notes                   "notes de version"
        :reports                         "rapports"
        :reset-password                  "réinitialiser le mot de passe"
        :reset-password-inst             "Entrez votre nom d'utilisateur pour réinitialiser votre mot de passe. Nous vous enverrons un email avec les instructions."
        :reset-password-error            "Erreur lors de la réinitialisation du mot de passe"
        :resource-type                   "type de ressource"
        :results                         "résultats"
        :return-code                     "code de retour"
        :rights                          "droits"
        :rights-icon                     "Icône représentant les droits"
        :save                            "sauvegarder"
        :search                          "chercher"
        :select                          "sélectionner"
        :select-application              "sélectionner une application"
        :select-datasets                 "cliquez sur les cartes pour sélectionner ou desélectionner les collections de données"
        :select-file                     "choisir un fichier"
        :select-logo-url                 "Séléctionner l'URL du logo"
        :session                         "session actuelle"
        :session-expires                 "session se termine"
        :settings                        "paramètres"
        :show-versions                   "montrer les versions"
        :source-code-on                  "code source sur"
        :success                         "Succès"
        :summary                         "résumé"
        :support                         "support"
        :signup                          "s'inscrire"
        :signup-failed                   "l'inscription a échoué"
        :signup-link                     "S'inscrire."
        :size                            "taille"
        :start                           "début"
        :state                           "état"
        :statistics                      "statistiques"
        :status                          "statut"
        :stop                            "stop"
        :tags                            "mots clés"
        :tech-doc                        "documentation technique"
        :terminate                       "terminer"
        :timestamp                       "horodatage"
        :to                              "à"
        :today                           "aujourd'hui"
        :total                           "total"
        :tutorials                       "tutoriels"
        :type                            "type"
        :unauthorized                    "non autorisé"
        :update                          "mettre à jour"
        :url                             "URL"
        :urls                            "URLs"
        :username                        "nom d'utilisateur"
        :users                           "utilisateurs"
        :version-number                  "Numéro de version"
        :validation-email-success-msg    "Un message de validation a été envoyé sur votre compte email."
        :value                           "valeur"
        :view                            "voir"
        :vms                             "VMs"
        :volumes                         "Volumes"
        :welcome                         "bienvenue"
        :welcome-api-desc                "Decouvrir et experimenter l'API"
        :welcome-application-desc        "Gérer les applications, composants et images. Partager vos applications en un clic"
        :welcome-appstore-desc           "Feuilleter les apps, composants et images publiés et partagés"
        :welcome-dashboard-desc          "Aller à l'essantiel en un clin d'oeil"
        :welcome-data-desc               "Lire vos datasets et lancer vos analyses de données "
        :welcome-deployment-desc         "Visualiser et contrôler l'ensemble de vos applications deployées sur un cloud"
        :welcome-detail                  "La solution complète pour gérer votre continuum multi-cloud et edge"
        :welcome-docs-desc               "Apprenez l'API des ressources en utilisant la documentation des API."
        :welcome-nuvlabox-desc           "Gérer vos NuvlaBoxes de façon centralisée"
        :yes                             "oui"
        :yesterday                       "hier"}})
