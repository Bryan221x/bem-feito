package com.bemfeito.api.admin;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.jdbc.core.JdbcTemplate;
import com.bemfeito.api.common.dto.PageResponse;
import com.bemfeito.api.common.domain.InstitutionStatus;
import static com.bemfeito.api.common.validation.RequestValidation.require;
@Service
@Transactional(readOnly=true)
@PreAuthorize("@institutionAuthorization.isAdmin()")
public class AdminQueryService {
    private final JdbcTemplate jdbc;
    public AdminQueryService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public AdminOutput.Summary summary() {
        return new AdminOutput.Summary(count("institutions"),jdbc.queryForObject("select count(*) from institution_memberships where status='ACTIVE'",Long.class),count("units"),
            jdbc.query("select c.name,s.uf,count(distinct u.institution_id) as total from units u join cities c on c.id=u.city_id join states s on s.id=c.state_id group by c.id,c.name,s.uf order by total desc,c.name,c.id limit 10",
                (rs,n)->new AdminOutput.CityCount(rs.getString(1),rs.getString(2),rs.getLong(3))));
    }
    private long count(String table) { return jdbc.queryForObject("select count(*) from "+table,Long.class); }
    public PageResponse<AdminOutput.InstitutionRow> institutions(String q,InstitutionStatus status,int page,int size) {
        require(page>=0 && size>=1 && size<=100,"Paginação inválida");
        require(q.length()<=150,"Busca muito longa");
        String pattern="%"+q.trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        String filter=" where (lower(i.display_name) like ? escape '!' or i.cnpj like ? escape '!')";
        List<Object> args=new ArrayList<>(List.of(pattern,pattern));
        if(status!=null) { filter+=" and i.status=?"; args.add(status.name()); }
        long total=jdbc.queryForObject("select count(*) from institutions i"+filter,Long.class,args.toArray());
        String sql="select i.id,i.display_name,i.cnpj,i.status,i.created_at,"+
            "(select count(*) from units u where u.institution_id=i.id) unit_count,"+
            "(select count(*) from institution_memberships m where m.institution_id=i.id and m.status='ACTIVE') member_count,"+
            "(select min(concat(c.name,' / ',s.uf)) from units u join cities c on c.id=u.city_id join states s on s.id=c.state_id where u.institution_id=i.id) location "+
            "from institutions i"+filter+" order by i.display_name,i.id limit ? offset ?";
        args.add(size);args.add((long)page*size);
        var rows=jdbc.query(sql,(rs,n)->new AdminOutput.InstitutionRow(rs.getLong("id"),rs.getString("display_name"),rs.getString("cnpj"),InstitutionStatus.valueOf(rs.getString("status")),rs.getTimestamp("created_at").toLocalDateTime(),rs.getLong("unit_count"),rs.getLong("member_count"),rs.getString("location")),args.toArray());
        return new PageResponse<>(rows,page,size,total,(int)((total+size-1)/size));
    }
}
