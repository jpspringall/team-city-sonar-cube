using Dapper;

namespace WebApplicationForTesting
{
    public class DbReader : IDisposable
    {
        private MiniProfilerConnection _miniProfilerConnection = new();

        //private IDbConnection GetConnection()
        //{
        //    DbConnection connection =

        //        new StackExchange.Profiling.Data.ProfiledDbConnection(new System.Data.SqlClient.SqlConnection(
        //            "Server=localhost,1533;Database=StackOverflow2010;User Id=sa;Password=P@ssw0rd;"), MiniProfiler.Current);
        //    //connection.Open();
        //    return connection;
        //}

        public void RunQuery()
        {
            using (var connection = _miniProfilerConnection.GetConnection())
            //using (var connection = GetConnection())
            {
                connection.Query("Select top 100 * from posts", new { categoryID = 1 });
            }
        }

        public void Dispose()
        {
            _miniProfilerConnection?.Dispose();
        }
    }
}
