/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2019.                            (c) 2019.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*                                       
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*                                       
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*                                       
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*                                       
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*                                       
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
*
************************************************************************
*/


package ca.nrc.cadc.caom2.artifact.resolvers;

import ca.nrc.cadc.caom2.artifact.resolvers.util.ResolverUtil;
import ca.nrc.cadc.net.StorageResolver;
import ca.nrc.cadc.net.Traceable;
import ca.nrc.cadc.util.StringUtil;

import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * StorageResolver implementation for the ALMA archive.
 * This class can convert an ALMA URI into a URL. The conversion is delegated to the AdResolver.
 *
 * @author yeunga
 */
public class CadcAlmaResolver implements StorageResolver, Traceable {
    public static final String SCHEME = "alma";
    private static final Logger log = Logger.getLogger(CadcAlmaResolver.class);

    private void sanitize(String s)
    {
        Pattern regex = Pattern.compile("^[a-zA-Z 0-9\\_\\.\\,\\-\\+\\@]*$");
        Matcher matcher = regex.matcher(s);
        if (!matcher.find())
            throw new IllegalArgumentException("Invalid dataset characters.");
    }
   
    private String removeNamespace(final URI uri) {
        String archive = null;
        String fileID = null;
        String parts = uri.getRawSchemeSpecificPart();
        int i = parts.indexOf('/');
        if (i > 0) {
            archive = parts.substring(0,i);
            parts = parts.substring(i+1); // namespace+fileID
        }

        i = parts.lastIndexOf('/');
        if (i > 0) {
            fileID = parts.substring(i+1);
        } else {
            fileID = parts;
        }
        
        if ( !StringUtil.hasText(archive) ) {
            throw new IllegalArgumentException("cannot extract archive from " + uri);
        }
        
        if ( !StringUtil.hasText(fileID) ) {
            throw new IllegalArgumentException("cannot extract fileID from " + uri);
        }

        sanitize(archive);
        sanitize(fileID);

        // trim the archive to 6 characters
        // TODO: Remove this trim when AD supports longer archive names
        if (archive.length() > 6) {
            archive = archive.substring(0, 6);
        }
        
        return archive + "/" + fileID;
    }
    
    @Override
    public URL toURL(URI uri) {
        ResolverUtil.validate(uri, SCHEME);

        try {
            AdResolver adResolver = new AdResolver();
            return adResolver.toURL(URI.create(AdResolver.SCHEME + ":" + removeNamespace(uri)));
        } catch (Throwable t) {
            String message = "Failed to convert to data URL";
            throw new RuntimeException(message, t);
        }
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }
}
