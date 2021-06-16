import subprocess, json, os, platform, sys, getopt
import xml.dom.minidom

API_VERSION = '51.0'

NAMESPACE = {
    'salesforce' : 'http://soap.sforce.com/2006/04/metadata',
    'xsi'        : 'http://www.w3.org/2001/XMLSchema-instance'
}

SFDX_EXECUTABLE = '/usr/local/bin/sfdx' if platform.system() == 'Linux' else 'sfdx'

def getParameters(argv):
    parameters = {}
    parameters['checkonly']       =  False
    parameters['outputdirectory'] = 'flow-destructive-changes/'
    parameters['targetusername']  = None

    try:
        opts, args = getopt.getopt(argv, 'hco:t:', ['help','checkonly','outputdirectory=','targetusername='])
    except getopt.GetoptError:
        print 'delete-old-flows.py --checkonly --outputdir <folder_name> --targetusername <my_sfdx_alias>'
        print 'delete-old-flows.py --checkonly --outputdir <folder_name> --targetusername <my_sfdx_alias>'
        sys.exit(2)

    for opt, arg in opts:
        if opt in ('-h', '--help'):
            print '''
                -c, --checkonly\tRuns the SFDX deploy command as a validation-only deployment. Defaults to False.
                -o, --outputdirectory\tSubdirectory to use for destructiveChanges.xml and package.xml. Defaults to flow-destructive-changes/.
                -t, --targetusername\tSFDX alias of the username for the desired environment. Your default username is used if not specified.
            '''
            sys.exit()
        elif opt in ('-c', '--checkonly'):
            parameters['checkonly'] = True
        elif opt in ('-o', '--outputdirectory'):
            if arg.endswith('/') == False:
                arg = arg + '/'
            parameters['outputdirectory'] = arg
        elif opt in ('-t', '--targetusername'):
            parameters['targetusername'] = arg

    print parameters
    return parameters

def getObsoleteFlowVersions(target_username):
    print 'Retrieving the list of obsolete flow versions'

    target_username_parameter = ' --targetusername ' + target_username + ' ' if target_username is not None else ''

    query = "SELECT Id, Status, Definition.DeveloperName, VersionNumber FROM Flow WHERE Definition.NamespacePrefix = null AND Status = 'Obsolete' ORDER BY Definition.DeveloperName, VersionNumber"
    query_command = SFDX_EXECUTABLE + ' force:data:soql:query --usetoolingapi --json --query "' + query + '"' + target_username_parameter
    print 'query_command=' + query_command

    response = json.loads(subprocess.check_output(query_command, shell=True))
    print response

    return response['result']['records']

def createPackageXml(output_directory):
    doc = xml.dom.minidom.Document()

    root = doc.createElementNS(NAMESPACE.get('salesforce'), 'Package')
    root.setAttribute('xmlns', NAMESPACE.get('salesforce'))
    doc.appendChild(root)

    version = doc.createElement('version')
    versionName = doc.createTextNode(API_VERSION)
    version.appendChild(versionName)
    root.appendChild(version)

    print(doc.toprettyxml())

    doc.writexml(
        open(output_directory + 'package.xml', 'w'),
        addindent = '    ',
        newl = '\n'
    )

def createFlowDestructiveChangesXml(output_directory, obsolete_flow_versions):
    doc = xml.dom.minidom.Document()

    root = doc.createElementNS(NAMESPACE.get('salesforce'), 'Package')
    root.setAttribute('xmlns', NAMESPACE.get('salesforce'))
    doc.appendChild(root)

    typesNode = doc.createElement('types')
    root.appendChild(typesNode)

    for flow in obsolete_flow_versions:
        print('flow==', flow)
        versionName = flow['Definition']['DeveloperName'] + '-' + str(flow['VersionNumber'])

        flowMemberNode = doc.createElement('members')
        flowNameNode   = doc.createTextNode(versionName)
        flowMemberNode.appendChild(flowNameNode)
        typesNode.appendChild(flowMemberNode)

    metadataTypeNode = doc.createElement('name')
    metadataTypeName = doc.createTextNode('Flow')
    metadataTypeNode.appendChild(metadataTypeName)
    typesNode.appendChild(metadataTypeNode)

    version = doc.createElement('version')
    versionName = doc.createTextNode(API_VERSION)
    version.appendChild(versionName)
    root.appendChild(version)

    print(doc.toprettyxml())

    doc.writexml(
        open(output_directory + 'destructiveChanges.xml', 'w'),
        addindent = "    ",
        newl = '\n'
    )

def deployDestructiveChanges(output_directory, target_username, check_only):
    target_username_parameter = ' --targetusername ' + target_username + ' ' if target_username is not None else ''
    check_only_parameter      = ' --checkonly ' if check_only else ''

    deploy_command = (
        SFDX_EXECUTABLE + ' force:mdapi:deploy '
        + ' --deploydir ' + output_directory
        + target_username_parameter
        + check_only_parameter
    )

    print('deploy_command=' + deploy_command)
    result = subprocess.check_output(deploy_command, shell=True)

if __name__ == '__main__':
    print 'Starting flow cleanup job'

    # Get the parameters
    parameters = getParameters(sys.argv[1:])

    output_directory = parameters['outputdirectory']
    check_only       = parameters['checkonly']
    target_username  = parameters['targetusername']

    # Make sure the output directory exists
    if not os.path.exists(output_directory):
        os.makedirs(output_directory)

    # Get the flows and generate the XML files
    flows = getObsoleteFlowVersions(target_username)
    createPackageXml(output_directory)
    createFlowDestructiveChangesXml(output_directory, flows)

    # Delete!
    deployDestructiveChanges(output_directory, target_username, check_only)

    print 'Finished flow cleanup job'