using System.Data;
using System.Data.Common;
using StackExchange.Profiling;

namespace WebApplicationForTesting
{
    public class MiniProfilerConnection : IDisposable
    {
        public IDbConnection ProfiledConnection;
        public IDbConnection GetConnection()
        {
            DbConnection connection = new System.Data.SqlClient.SqlConnection(
                "Server=localhost,1533;Database=StackOverflow2010;User Id=sa;Password=P@ssw0rd;");
            ProfiledConnection = new StackExchange.Profiling.Data.ProfiledDbConnection(connection, MiniProfiler.Current);
            ProfiledConnection.Open();
            return ProfiledConnection;
        }

        public void Dispose()
        {
            ProfiledConnection?.Dispose();
        }
    }
}
