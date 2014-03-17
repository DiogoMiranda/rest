package br.com.brejaonline.services;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang.StringUtils;

import br.com.brejaonline.model.Cerveja;
import br.com.brejaonline.model.CervejaJaExisteException;
import br.com.brejaonline.model.Estoque;
import br.com.brejaonline.model.rest.Cervejas;

import static br.com.brejaonline.util.Hash.hash;

@Path("/cervejas")
@Consumes({ MediaType.TEXT_XML, MediaType.APPLICATION_XML,
		MediaType.APPLICATION_JSON })
@Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML,
		MediaType.APPLICATION_JSON })
@Cached
public class CervejaService {

	private static Estoque estoque = new Estoque();

	private static final int TAMANHO_PAGINA = 1;

	@GET
	@Path("{nome}")
	public Response encontreCerveja(@PathParam("nome") String nomeDaCerveja) {
		Cerveja cerveja = estoque.recuperarCervejaPeloNome(nomeDaCerveja);
		if (cerveja != null) {
			return Response.ok(cerveja).tag(hash(cerveja)).build();
		}

		throw new WebApplicationException(Status.NOT_FOUND);

	}

	@GET
	public Response listeTodasAsCervejas(@QueryParam("pagina") int pagina) {

		List<Cerveja> cervejasList = estoque.listarCervejas(pagina,
				TAMANHO_PAGINA);

		Cervejas cervejas = new Cervejas(cervejasList);

		return Response.ok(cervejas).tag(hash(cervejas)).build();
	}

	@POST
	public Response criarCerveja(Cerveja cerveja) {
		try {
			estoque.adicionarCerveja(cerveja);
		} catch (CervejaJaExisteException e) {
			throw new WebApplicationException(Status.CONFLICT);
		}

		URI uri = UriBuilder.fromPath("cervejas/{nome}").build(
				cerveja.getNome());

		return Response.created(uri).entity(cerveja).build();
	}

	@PUT
	@Path("{nome}")
	public void atualizarCerveja(@PathParam("nome") String nome,
			@HeaderParam("If-Match") String eTag, Cerveja cerveja) {
		testaETag(eTag, nome);
		cerveja.setNome(nome);
		estoque.atualizarCerveja(cerveja);
	}

	@DELETE
	@Path("{nome}")
	public void apagarCerveja(@HeaderParam("If-Match") String eTag,
			@PathParam("nome") String nome) {
		testaETag(eTag, nome);
		estoque.apagarCerveja(nome);
	}

	@GET
	@Path("{nome}")
	@Produces("image/*")
	public Response recuperaImagem(@PathParam("nome") String nomeDaCerveja)
			throws IOException {
		InputStream is = CervejaService.class.getResourceAsStream("/"
				+ nomeDaCerveja + ".jpg");

		if (is == null)
			throw new WebApplicationException(Status.NOT_FOUND);

		byte[] dados = new byte[is.available()];
		is.read(dados);
		is.close();

		return Response.ok(dados).tag(hash(dados)).type("image/jpg").build();
	}

	private static Map<String, String> EXTENSOES;

	static {
		EXTENSOES = new HashMap<>();
		EXTENSOES.put("image/jpg", ".jpg");
	}

	@POST
	@Path("{nome}")
	@Consumes("image/*")
	public Response criaImagem(@PathParam("nome") String nomeDaImagem,
			@Context HttpServletRequest req, byte[] dados) throws IOException,
			InterruptedException {

		String userHome = System.getProperty("user.home");
		String mimeType = req.getContentType();
		FileOutputStream fos = new FileOutputStream(userHome
				+ java.io.File.separator + nomeDaImagem
				+ EXTENSOES.get(mimeType));

		fos.write(dados);
		fos.flush();
		fos.close();

		return Response.ok().build();
	}

	private void testaETag(String eTag, String nomeCerveja) {
		Response responseEncontraCerveja = encontreCerveja(nomeCerveja);
		
		
		if (StringUtils.isNotEmpty(eTag)
				&& !responseEncontraCerveja.getEntityTag().getValue()
						.equals(eTag)) {

			throw new WebApplicationException(Status.CONFLICT);

		}
	}
}
